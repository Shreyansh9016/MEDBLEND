import React, { useEffect, useState, useCallback } from "react";
import { initializeApp } from "firebase/app";
import { getDatabase, ref, onValue } from "firebase/database";
import { Line } from "react-chartjs-2";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
} from "chart.js";
import { useTheme } from "@mui/material";
import { ToastContainer, toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import { useNavigate } from "react-router-dom";
import { tokens } from "../../theme";

// Firebase Config
const firebaseConfig = {
  apiKey: "AIzaSyBhWqs6RQJljhHmQPocdEZtd7Zsg9hZ4P8",
  authDomain: "crickstats-b10ee.firebaseapp.com",
  databaseURL: "https://crickstats-b10ee-default-rtdb.firebaseio.com",
  projectId: "crickstats-b10ee",
  storageBucket: "crickstats-b10ee.appspot.com",
  messagingSenderId: "114862367513",
  appId: "1:114862367513:android:20db76f6091c668af1db88"
};

// Initialize Firebase
initializeApp(firebaseConfig);
const database = getDatabase();

// Register the scales for Chart.js
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
);

const Dashboard = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  const [elderData, setElderData] = useState(null);
  const [weeklyHealthData, setWeeklyHealthData] = useState({});
  const userKey = localStorage.getItem("userKey");
  const navigate = useNavigate();

  // Logout handler
  const handleLogout = useCallback(() => {
    localStorage.removeItem("userKey");
    setElderData(null);
    toast.info("Logged out successfully.");
    navigate("/");
    window.location.reload();
  }, [navigate]);

  // Fetch elder data from Firebase
  useEffect(() => {
    if (userKey) {
      const dataRef = ref(database, `Users/${userKey}`);
      onValue(dataRef, (snapshot) => {
        const data = snapshot.val();
        if (data) {
          setElderData(data);
          setWeeklyHealthData(data.weeklyHealthData || {});
        }
      });
    }
  }, [userKey]);

  // Function to play notification sound
  const playSound = useCallback(() => {
    const sound = new Audio("/mm.mp3");
    sound.preload = 'auto';
    sound.play().catch((err) => console.log('Error playing sound:', err));
  }, []);

  // Function to calculate the next dose time
  const getNextDose = useCallback(() => {
    if (!elderData?.medicines) return "No medicines.";

    const now = new Date();
    const allTimes = [];
    let upcomingDose = null;

    // Collect all medicine times
    Object.values(elderData.medicines).forEach((medicine) => {
      if (medicine.times) {
        Object.values(medicine.times).forEach((time) => {
          allTimes.push(time);
        });
      }
    });

    // Sort times and find the next dose time
    const sortedTimes = allTimes.sort();
    const nextTime = sortedTimes.find((time) => {
      const [hours, minutes] = time.split(":").map(Number);
      const doseTime = new Date();
      doseTime.setHours(hours, minutes, 0, 0);

      return doseTime > now; // Find the next dose time after the current time
    });

    if (nextTime) {
      const [hours, minutes] = nextTime.split(":").map(Number);
      const doseTime = new Date();
      doseTime.setHours(hours, minutes, 0, 0);

      const diffInMinutes = (doseTime - now) / (1000 * 60); // Difference in minutes

      if (diffInMinutes <= 1 && diffInMinutes > 0) {
        playSound();

        // Set interval to repeat sound every 30 seconds until the dose time is reached
        const soundInterval = setInterval(() => {
          const updatedDiffInMinutes = (doseTime - new Date()) / (1000 * 60);
          if (updatedDiffInMinutes <= 0) {
            clearInterval(soundInterval);
          } else {
            playSound();
          }
        }, 30000);

        toast.info(`Time for your next dose: ${nextTime}`, {
          autoClose: 4000,
          position: "top-center",
        });
      }

      return nextTime; // Return next dose time
    } else {
      return "All doses done for today!";
    }
  }, [elderData, playSound]);

  useEffect(() => {
    if (elderData?.medicines) {
      // Check every minute for upcoming dose
      const interval = setInterval(() => {
        getNextDose();
      }, 60000); // Check every minute

      return () => clearInterval(interval); // Cleanup on unmount
    }
  }, [elderData, getNextDose]);

  // Weekly chart data preparation
  const weeklyChartData = {
    labels: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
    datasets: [
      {
        label: "Blood Pressure (BP)",
        data: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].map((day) =>
          weeklyHealthData[day]?.bp ? parseInt(weeklyHealthData[day].bp) : 0
        ),
        fill: true,
        backgroundColor: "rgba(99, 102, 241, 0.2)",
        borderColor: "rgb(99, 102, 241)",
      },
      {
        label: "Pre-Meal Sugar Level",
        data: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].map((day) =>
          weeklyHealthData[day]?.diabetesPre ? parseInt(weeklyHealthData[day].diabetesPre) : 0
        ),
        fill: true,
        backgroundColor: "rgba(16, 185, 129, 0.2)",
        borderColor: "rgb(16, 185, 129)",
      },
      {
        label: "Post-Meal Sugar Level",
        data: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].map((day) =>
          weeklyHealthData[day]?.diabetesPost ? parseInt(weeklyHealthData[day].diabetesPost) : 0
        ),
        fill: true,
        backgroundColor: "rgba(255, 99, 132, 0.2)",
        borderColor: "rgb(255, 99, 132)",
      },
    ],
  };

  return (
    <div className="mainContainer">
      <ToastContainer />
      <div className="header">
        <h1 className="title">Welcome to {elderData?.name || "N/A"}'s Medical Dashboard! 🎉</h1>
      </div>

      {elderData ? (
        <div className="dashboard-grid">

          {/* Patient Info */}
          <div className="card">
            <h2>Patient Profile</h2>
            <p><strong>Name:</strong> {elderData.name}</p>
            <p><strong>Username:</strong> {elderData.username}</p>
            <p><strong>Next Dose:</strong> {getNextDose()}</p>
          </div>

          {/* Medicines */}
          <div className="card">
            <h2>Medicines</h2>
            {elderData.medicines ? (
              Object.values(elderData.medicines).map((medicine, idx) => (
                <div key={idx} className="medicine-card">
                  <div className="medicine-details">
                    <h3>Medicine Name: {medicine.medicineName}</h3>
                    <p><strong>No. of Doses:</strong> {medicine.noOfDoses}</p>
                    <p><strong>Times:</strong></p>
                    <ul>
                      {medicine.times ? Object.values(medicine.times).map((time, id) => (
                        <li key={id}>{time}</li>
                      )) : <li>No times specified</li>}
                    </ul>
                  </div>
                  <img src="/assets/image.png" alt="Medicine" width={230} />
                </div>
              ))
            ) : (
              <p>No medicines found.</p>
            )}
          </div>

          {/* Weekly Graph */}
          <div className="card">
            <h2>Weekly Health Track</h2>
            <Line data={weeklyChartData} />
          </div>

        </div>
      ) : (
        <p>Loading your data...</p>
      )}

      <button className="logout-button" onClick={handleLogout}>Logout</button>
    </div>
  );
};

export default Dashboard;
