export interface RegisterFormData {
    email: string;
    password: string;
    confirmPassword: string;
    name: string;
    phoneNumber: number;
    dob: Date;
    gender: string;
    country: string;
    image: string;
  }
  
  export interface CountryData {
    [id: string]: string;
  }