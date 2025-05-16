package com.shelf_sense_backend.service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shelf_sense_backend.model.Order;
import com.shelf_sense_backend.model.ProcessedEmail;
import com.shelf_sense_backend.model.ShoppedItem;
import com.shelf_sense_backend.repo.OrderRepository;
import com.shelf_sense_backend.repo.ProcessedEmailRepository;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;

@Service
public class OrderEmailProcessingService {

    private static final Logger log = LoggerFactory.getLogger(OrderEmailProcessingService.class);

    // @Autowired
    // private TranslationService translationService;
    // @Value("${app.email.translation.source-language:no}")
    // private String sourceLanguage;
    // @Value("${app.email.translation.target-language:en}")
    // private String targetLanguage;
    // @Value("${app.email.translation.enabled:false}")
    // private boolean translationEnabled;

    private static final Pattern ORDER_NUMBER_PATTERN = Pattern.compile(
            "Oda:\\s*Kvittering\\s+([\\w\\d]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORDERED_ITEMS_PATTERN = Pattern.compile(
            "Bestilte varer|Ordered Items", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUMMARY_PATTERN = Pattern.compile(
            "Summary|Total|Oppsummering", Pattern.CASE_INSENSITIVE);

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private ProcessedEmailRepository processedEmailRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private GroceryReceiptAnalyzer groceryReceiptAnalyzer;

    @Value("${spring.mail.username}")
    private String username;
    @Value("${spring.mail.password}")
    private String password;
    @Value("${spring.mail.host}")
    private String host;
    @Value("${spring.mail.properties.mail.store.protocol}")
    private String protocol;
    @Value("${app.email.polling.subject-filter}")
    private String subjectFilter;
    @Value("${app.email.polling.folder}")
    private String folderName;
    @Value("${app.email.polling.mark-processed-as-read:false}")
    private boolean markProcessedAsRead;
    @Value("${app.email.polling.cron-schedule}")
    private String cronSchedule;

    // @Scheduled(cron = "${app.email.polling.cron-schedule}")
    @Transactional
    public void pollAndProcessEmails() {
        log.info("Starting scheduled email polling task (Cron: {})...", cronSchedule);
        Store store = null;
        Folder emailFolder = null;

        try {

            Session emailSession = Session.getInstance(((JavaMailSenderImpl) mailSender).getJavaMailProperties());
            store = emailSession.getStore(protocol);
            store.connect(host, username, password);
            log.debug("Connected to email store: {}@{}", protocol, host);

            emailFolder = store.getFolder("INBOX");

            emailFolder.open(Folder.READ_WRITE);
            log.debug("Opened folder: '{}'", folderName);

            SearchTerm subjectTerm = new SubjectTerm(subjectFilter);
            Message[] messages = emailFolder.search(subjectTerm);
            log.info("Found {} email(s) with subject containing '{}' in folder '{}'.",
                    messages.length, subjectFilter, folderName);

            if (messages.length == 0) {
                log.info("No new emails to process.");
                return;
            }

            List<Message> filteredMessages = getFilteredMessages(List.of(messages));

            log.info("After date filtering: {} email(s) from 2025 or later to process.", filteredMessages.size());

            if (filteredMessages.isEmpty()) {
                log.info("No eligible emails to process after filtering.");
                return;
            }

            for (Message message : filteredMessages) {
                String messageId = "<unknown>";
                try {
                    messageId = getMessageId(message);
                    if (messageId == null) {
                        log.warn("Email with Subject '{}' is missing Message-ID header. Skipping.",
                                message.getSubject());
                        continue;
                    }

                    log.info("Processing email - Subject: '{}', Sent: '{}', Message-ID: {}",
                            message.getSubject(), message.getSentDate(), messageId);

                    String orderNumber = extractOrderNumber(message.getSubject());
                    if (orderNumber == null) {
                        log.error("Could not extract Order Number from email Message-ID: {}.Skipping.", messageId);
                        continue;
                    }

                    if (processedEmailRepository.existsByMessageId(messageId)) {
                        log.warn(
                                "Email with Message-ID {} has already been processed (found in processed_emails table). Skipping.",
                                messageId);

                        if (markProcessedAsRead)
                            message.setFlag(Flags.Flag.SEEN, true);
                        continue;
                    }

                    String emailContent = getTextFromMessage(message);
                    if (emailContent.isBlank()) {
                        log.error("Extracted email content is blank for Message-ID: {}. Cannot process. Skipping.",
                                messageId);

                        continue;
                    }
                    // emailContent = translateEmailContentIfEnabled(emailContent);
                    // Order order = receiptOcrService.createOrderFromReceipt(emailContent);

                    if (orderRepository.existsByOrderNumber(orderNumber)) {
                        log.warn(
                                "An order with Order Number '{}' already exists in the database. Skipping processing items from email Message-ID: {}. This email will be marked as processed.",
                                orderNumber, messageId);

                        saveProcessedEmailRecord(messageId, message.getSentDate(), null);

                        if (markProcessedAsRead)
                            message.setFlag(Flags.Flag.SEEN, true);
                        continue;
                    }

                    LocalDate orderDate = extractInvoiceDate(emailContent);
                    if (message.getSentDate() == null) {
                        log.error("Could not extract Order Date from email Message-ID: {}. Skipping.", messageId);
                        continue;
                    }

                    String itemsBlock = extractItemsBlock(emailContent);
                    if (itemsBlock.isEmpty()) {
                        log.error(
                                "Could not extract items block between 'Ordered items' and 'Summary' for Order: {}, Message-ID: {}. Skipping order save.",
                                orderNumber, messageId);
                        continue;
                    }

                    Order order = new Order();
                    order.setOrderNumber(orderNumber);
                    order.setOrderDate(orderDate);

                    List<ShoppedItem> items = groceryReceiptAnalyzer.parseReceipt(emailContent, order);

                    if (items.isEmpty()) {
                        log.error(
                                "No items found in the email content for Order: {}, Message-ID: {}. Skipping order save.",
                                orderNumber, messageId);
                        continue;
                    }

                    log.info("Attempting to save new Order: Number={}, Date={}, Items={}",
                            orderNumber, orderDate,
                            items.size());

                    ProcessedEmail processedEmailRecord = saveProcessedEmailRecord(messageId, message.getSentDate(),
                            null);
                    order.setProcessedEmail(processedEmailRecord);

                    for (ShoppedItem item : items) {
                        order.addItem(item);
                    }

                    Order savedOrder = orderRepository.save(order);
                    processedEmailRecord.setGeneratedOrder(savedOrder);
                    processedEmailRepository.save(processedEmailRecord);

                    log.info("Successfully saved Order ID: {} (Number: {}) with {} items from Message-ID: {}",
                            savedOrder.getId(), savedOrder.getOrderNumber(), savedOrder.getItems().size(), messageId);

                    if (markProcessedAsRead) {
                        message.setFlag(Flags.Flag.SEEN, true);
                        log.debug("Marked email {} as read in mailbox.", messageId);
                    }

                } catch (Exception e) {
                    log.error(
                            "Critical error processing email Message-ID: {}. Error: {}. Check email content and parser logic.",
                            messageId, e.getMessage(), e);

                }
            }

        } catch (AuthenticationFailedException e) {
            log.error("EMAIL AUTHENTICATION FAILED! Verify username/password/app password in configuration.", e);

        } catch (MessagingException e) {
            log.error("MessagingException during email polling: {}. Check connection settings or mailbox state.",
                    e.getMessage(), e);
        } catch (Exception e) {

            log.error("Unexpected error during email polling task: {}", e.getMessage(), e);
        } finally {

            closeFolderAndStore(emailFolder, store);
            log.info("Finished email polling task run.");
        }
    }

    private ProcessedEmail saveProcessedEmailRecord(String messageId, Date sentDateRaw, Order relatedOrder) {
        ProcessedEmail record = new ProcessedEmail();
        record.setMessageId(messageId);

        Instant sentTimestamp = (sentDateRaw != null) ? sentDateRaw.toInstant() : Instant.now();

        record.setEmailSentTimestamp(sentTimestamp);
        record.setProcessedTimestamp(Instant.now());
        record.setGeneratedOrder(relatedOrder);
        return processedEmailRepository.save(record);
    }

    private String extractOrderNumber(String subject) {
        Matcher matcher = ORDER_NUMBER_PATTERN.matcher(subject);
        if (matcher.find()) {
            String orderNumber = matcher.group(1).trim();
            log.debug("Successfully extracted order number: {} from subject: {}", orderNumber, subject);
            return orderNumber;
        }

        log.warn("Could not extract order number from subject: {}", subject);
        return null;
    }

    private LocalDate extractInvoiceDate(String content) {
        Pattern pattern = Pattern.compile("Fakturadato\\s+(\\d{1,2})\\.\\s+(\\w+)\\s+(\\d{4})");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            int day = Integer.parseInt(matcher.group(1));
            String norwegianMonth = matcher.group(2).toLowerCase();
            int year = Integer.parseInt(matcher.group(3));

            int month = convertNorwegianMonthToNumber(norwegianMonth);
            if (month == -1) {
                return null; 
            }

            LocalDate date = LocalDate.of(year, month, day);
            return date; 
        }
        return null;
    }

    private int convertNorwegianMonthToNumber(String norwegianMonth) {
        return switch (norwegianMonth) {
            case "januar" -> 1;
            case "februar" -> 2;
            case "mars" -> 3;
            case "april" -> 4;
            case "mai" -> 5;
            case "juni" -> 6;
            case "juli" -> 7;
            case "august" -> 8;
            case "september" -> 9;
            case "oktober" -> 10;
            case "november" -> 11;
            case "desember" -> 12;
            default -> -1;
        };
    }

    private String extractItemsBlock(String emailContent) {

        Matcher startMatcher = ORDERED_ITEMS_PATTERN.matcher(emailContent);
        if (!startMatcher.find()) {
            log.warn("Could not find 'Ordered items' marker in email content.");
            return "";
        }

        Matcher endMatcher = SUMMARY_PATTERN.matcher(emailContent);
        if (!endMatcher.find()) {
            log.warn("Could not find 'Summary' or 'Total' marker in email content.");
            int approximateEnd = Math.min(startMatcher.end() + 3000, emailContent.length());
            return emailContent.substring(startMatcher.end(), approximateEnd).trim();
        }
        if (startMatcher.end() >= endMatcher.start()) {
            log.warn("'Ordered items' marker found at or after 'Summary' marker. Invalid format.");
            return "";
        }

        int itemsSectionStart = emailContent.indexOf('\n', startMatcher.start());
        if (itemsSectionStart == -1 || itemsSectionStart > endMatcher.start()) {
            itemsSectionStart = startMatcher.end();
        } else {
            itemsSectionStart += 1;
        }

        log.debug("Extracting items block from index {} to {}", itemsSectionStart, endMatcher.start());
        return emailContent.substring(itemsSectionStart, endMatcher.start()).trim();
    }

    private String getMessageId(Message message) throws MessagingException {
        if (message instanceof MimeMessage) {
            return ((MimeMessage) message).getMessageID();
        }

        String[] headers = message.getHeader("Message-ID");
        if (headers != null && headers.length > 0) {
            return headers[0];
        }
        return null;
    }

    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        }
        if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            return getTextFromMimeMultipart(mimeMultipart);
        }
        log.warn("Unsupported email content type: {}", message.getContentType());
        return "";
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        String textContent = null;
        String htmlContent = null;

        for (int i = 0; i < mimeMultipart.getCount(); i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);

            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                continue;
            }

            if (bodyPart.isMimeType("text/plain") && textContent == null) {
                textContent = bodyPart.getContent().toString();
                break;
            } else if (bodyPart.isMimeType("text/html") && htmlContent == null) {
                htmlContent = bodyPart.getContent().toString();
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                String nestedText = getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
                if (textContent == null && nestedText != null && !nestedText.isEmpty()) {
                    textContent = nestedText;
                }
            }
        }

        if (textContent != null) {
            log.trace("Extracted text/plain content.");
            return textContent;
        } else if (htmlContent != null) {
            log.warn("No text/plain content found, falling back to HTML content (basic stripping).");
            return htmlContent.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        } else {
            log.warn("Could not find text/plain or text/html part in multipart message.");
            return "";
        }
    }

    public List<Message> getFilteredMessages(List<Message> messages) throws MessagingException {
        List<Message> filteredMessages = new ArrayList<>();
        for (Message message : messages) {
            try {
                Date sentDate = message.getSentDate();
                if (sentDate != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(sentDate);
                    int year = cal.get(Calendar.YEAR);

                    if (year >= 2025) {
                        filteredMessages.add(message);
                    }
                }
            } catch (MessagingException e) {
                log.error("Error processing message date: {}", e.getMessage());
            }
        }

        return filteredMessages;
    }

    // private String translateEmailContentIfEnabled(String originalContent) {
    // if (!translationEnabled || originalContent == null ||
    // originalContent.isBlank()) {
    // return originalContent;
    // }

    // try {
    // log.info("Translating email content from {} to {}", sourceLanguage,
    // targetLanguage);
    // String translatedContent = translationService.translateText(
    // originalContent, sourceLanguage, targetLanguage);

    // if (translatedContent != null && !translatedContent.equals(originalContent))
    // {
    // log.debug("Email content successfully translated");
    // return translatedContent;
    // } else {
    // log.warn("Translation returned null or unchanged content");
    // return originalContent;
    // }
    // } catch (Exception e) {
    // log.error("Error during translation, using original content: {}",
    // e.getMessage(), e);
    // return originalContent;
    // }
    // }

    private void closeFolderAndStore(Folder folder, Store store) {
        try {
            if (folder != null && folder.isOpen()) {
                folder.close(false);
                log.debug("Closed email folder.");
            }
        } catch (MessagingException e) {
            log.error("Error closing email folder: {}", e.getMessage(), e);
        }
        try {
            if (store != null && store.isConnected()) {
                store.close();
                log.debug("Closed email store connection.");
            }
        } catch (MessagingException e) {
            log.error("Error closing email store: {}", e.getMessage(), e);
        }
    }
}