import "./registerpage.scss";
import { Controller, useForm } from "react-hook-form";
import DatePicker from "react-datepicker";

import { useCallback, useEffect, useState } from "react";
import "react-datepicker/dist/react-datepicker.css";
import { useNavigate } from "react-router-dom";
import api from "../../api/globalApi";
import { CountryData, RegisterFormData } from "./type";


const RegisterPage = () => {
  const navigate = useNavigate();

  const [countries, setCountries] = useState<CountryData>({});

  const getCountryData = useCallback(async () => {
    const response = await api.get(
      "/data/country.json"
    );
    console.log(response.data);
    setCountries(response.data);
    console.log(countries);
  }, [countries]);

  useEffect(() => {
    getCountryData();
  }, []);

  const {
    register,
    handleSubmit,
    getValues,
    setError,
    control,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormData>();

  const onSubmit = async (data: RegisterFormData) => {
    try {
      const {confirmPassword, ...rest} = data;
      console.log(rest);
      //http://127.0.0.1:8000/users/register  //for python backend
      const response = await api.post("auth/register", rest, {
        headers: {
          "Content-Type": "application/json",
        },
      });
      console.log(response.data);
      if (response.status === 409) {
        setError("root", { type: "manual", message: response.data.message });
        return false;
      }
      navigate("/shelf-sense/");
    } catch (error) {
      //setError("root", { type: "manual", message: "Something went wrong. Please try again later" });
      console.log(error);
    }
  };

  return (
    <div className="register">
      {" "}
      <h3>Register</h3>
      <div className="register-container">
        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="items">
            <input
              {...register("email", {
                required: "Email is required",
                pattern: {
                  value: /^[a-zA-Z0-9._%+-]+@gmail\.com$/,
                  message: "Email is incorrect format",
                },
              })}
              placeholder="Enter your email"
            />
            {errors.email && <div>{errors.email.message}</div>}
            <input
              {...register("password", {
                required: "Password is required",
                minLength: { value: 4, message: "Minimum length should be 4" },
              })}
              placeholder="Enter your password"
            />
            {errors.password && <div>{errors.password.message}</div>}

            <input
              {...register("confirmPassword", {
                required: "Confirm password is required",
                validate: (value) => {
                  if (value !== getValues("password"))
                    return "Passwords dont match";
                  return true;
                },
              })}
              placeholder="Confirm your password"
            />
            {errors.confirmPassword && (
              <div>{errors.confirmPassword.message} </div>
            )}

            <input
              {...register("name", {
                required: "Name is required",
              })}
              placeholder="Enter your name"
            />
            {errors.name && <div>{errors.name.message}</div>}

            <input
              type="number"
              {...register("phoneNumber", {
                required: "Phone number is required",
                minLength: { value: 8, message: "Wrong phone number" },
                maxLength: { value: 10, message: "Wrong phone number" },
              })}
              placeholder="Enter your phone number"
            />
            {errors.phoneNumber && <div>{errors.phoneNumber.message}</div>}

            <Controller
              rules={{ required: "Date is required" }}
              name="dob"
              control={control}
              defaultValue={undefined}
              render={({ field }) => (
                <DatePicker
                  selected={field.value}
                  onChange={(date) => field.onChange(date)}
                  placeholderText="Date of birth"
                  className="dob"
                  showYearDropdown
                  showMonthDropdown
                  dropdownMode="select"
                />
              )}
            ></Controller>
            {errors.dob && <div>{errors.dob.message} </div>}

            <input
              {...register("image", {
                required: "Name is required",
              })}
              placeholder="Enter your name"
            />
            {errors.image && <div>{errors.image.message}</div>}

            <div className="gender">
              <div className="genderItems">
                <input
                  {...register("gender", {
                    required: "Gender is required",
                  })}
                  value="male"
                  type="radio"
                />
                Male
              </div>
              <div className="genderItems">
                <input
                  {...register("gender", {
                    required: "Gender is required",
                  })}
                  value="female"
                  type="radio"
                />
                Female
              </div>
            </div>
            {errors.gender && <div>{errors.gender.message} </div>}

            <select
              className="country"
              {...register("country", {
                required: "Country is required",
              })}
            >
              <option>select</option>
              {Object.entries(countries).map(([id, name]) => (
                <option key={id} value={name}>
                  {name}
                </option>
              ))}
            </select>
            {errors.country && <div>{errors.country.message} </div>}

            <div className="rb">
              <button className="registerButton">
                {isSubmitting ? "Submitting...." : "Register"}
              </button>
            </div>
            {errors.root && (
              <div style={{ color: "red" }}>{errors.root.message}</div>
            )}
          </div>
        </form>
      </div>
    </div>
  );
};

export default RegisterPage;
