// postcss.config.js
export default {
  plugins: {
    '@tailwindcss/postcss': {}, // This is now correct - they've moved the plugin
    autoprefixer: {},
  },
}
