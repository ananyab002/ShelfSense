import { useForm } from "react-hook-form";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { useLogin } from "../../hooks/useLogin"; 
import { LoginDataType } from "../../context/type"; 

const LoginPage = () => {
    const navigate = useNavigate();
    const { loginCurrentUser, loginError } = useLogin();
    const [error, setError] = useState('');
    const location = useLocation();
    const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginDataType>();

    useEffect(() => {
        if (location.state?.authError) {
            setError(location.state.authError);
            window.history.replaceState({}, document.title);
        }
    }, [location.state]);

    const onSubmit = async (data: LoginDataType) => {
        setError('');
        const response = await loginCurrentUser(data);
        if (response?.success) {
            navigate("/shelf-sense/home"); 
        }
    };

    return (
            <div className="w-100  max-h-900 bg-[#111] rounded-[20px] text-white flex flex-col">
                <div className="p-5 m-[30px]">
                    <form onSubmit={handleSubmit(onSubmit)}>
                        <div className="flex flex-col items-center justify-center gap-6">
                            <div className="w-3/4">
                                <label htmlFor="email" className="sr-only">Email</label>
                                <input
                                    id="email"
                                    type="email"
                                    {...register("email", {
                                        required: "Email is required",
                                        pattern: { value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i, message: "Invalid email address" }
                                    })}
                                    placeholder="Enter your email"
                                    aria-invalid={errors.email ? "true" : "false"}
                                    className={`w-full p-2 border-0 bg-[#222] rounded-[15px] text-white 
                                        placeholder:text-gray-400 text-center focus:outline-none focus:ring-2 focus:ring-sky-500 
                                        ${errors.email ? 'ring-1 ring-red-500' : ''}`}
                                />
                                {errors.email && <p role="alert" className="mt-1 text-sm text-red-400 text-center">{errors.email.message}</p>}
                            </div>

                            <div className="w-3/4">
                                <label htmlFor="password" className="sr-only">Password</label>
                                <input
                                    id="password"
                                    type="password"
                                    {...register("password", {
                                        required: "Password is required",
                                        minLength: { value: 4, message: "Minimum 4 characters" }
                                    })}
                                    placeholder="Enter your password"
                                    aria-invalid={errors.password ? "true" : "false"}
                                    className={`w-full p-2 border-0 bg-[#222] rounded-[15px] text-white 
                                        placeholder:text-gray-400 text-center focus:outline-none focus:ring-2 focus:ring-sky-500
                                         ${errors.password ? 'ring-1 ring-red-500' : ''}`}
                                />
                                {errors.password && <p role="alert" className="mt-1 text-sm text-red-400 text-center">{errors.password.message}</p>}
                            </div>

                          
                            <button
                                type="submit"
                                disabled={isSubmitting}
                                className="w-[200px] py-2 border-0 bg-sky-400 hover:bg-sky-500 
                                text-white font-semibold rounded-lg disabled:opacity-60 disabled:cursor-not-allowed"
                            >
                                {isSubmitting ? "Logging in..." : "Login"}
                            </button>

                   
                            {(loginError || error) && (
                                <div className="mt-2 text-center text-sm text-red-400" role="alert">
                                    {loginError || error}
                                </div>
                            )}

                       
                            <p className="mt-4 text-center text-sm text-gray-400">
                                Don't have an account?{" "}
                                <Link
                                    to="/shelf-sense/register"
                                    className="font-medium text-sky-400 hover:text-sky-300 hover:underline"
                                >
                                    Register here
                                </Link>
                            </p>
                        </div>
                    </form>
                </div>
            </div>
    
    );
};

export default LoginPage;