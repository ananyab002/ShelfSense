import { createBrowserRouter, RouterProvider } from "react-router-dom";
import LoginPage from "../pages/LoginPage/LoginPage";
import RegisterPage from "../pages/RegisterPage/RegisterPage";
import HomePage from "../pages/HomePage/HomePage"
import ProtectedLayout from "../protectedLayout";



function Router() {
  const router = createBrowserRouter([
    {
      path: "/shelf-sense/",
      element: <LoginPage />,
    },
    {
      path: "/shelf-sense/register",
      element: <RegisterPage />,
    },
    {
      path: "/shelf-sense/home",
      element: <ProtectedLayout />, // Wrap protected routes
      children: [
        {
          index:true,
          element: <HomePage />,
          
        },
      ]
    }
  ]);
  return <RouterProvider router={router}></RouterProvider>;
}

export default Router;
