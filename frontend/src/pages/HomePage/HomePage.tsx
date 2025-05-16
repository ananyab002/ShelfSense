
//import { Outlet } from "react-router-dom";
import { PreviousOrders } from "../../components/previousOrders/PreviousOrders";
import "./homePage.scss";

const HomePage = () => {

  return (
    <div className="w-[90vw] h-[90vh] flex bg-[#111]">
      <PreviousOrders/>
    </div>
  );
};

export default HomePage;
