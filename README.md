# 🍼 EchoCare - Infant Cry Monitoring System (Frontend)
The EchoCare Android app is the companion interface for the EchoCare Backend, providing deaf and hard-of-hearing parents with real-time cry alerts, an analytics dashboard and educational resources all from their phone.

- 🎓 Final Year Project: - TU Dublin (TU856)
- 👩‍💻 Student: Danelle Pillay (C22348731)

## 🔗 Related Repositories
EchoCare Backend: https://github.com/danellejp/echocare-infant-cry-classification/

## 📖 About
The EchoCare app connects to a Raspberry Pi over a local WiFi network to receive real-time infant cry notifications. When the Pi detects and classifies a cry, the app instantly alerts the parent through push notifications, cry-type-specific vibration patterns and a camera torch flash. This ensures that no cry goes unnoticed, regardless of hearing ability.

## 📱 Features
- 🔔 Real-Time Cry Notifications: Instant alerts when your baby cries
- 📳 Cry-Type-Specific Vibration Patterns: Distinguish Hungry, Pain and Normal cries by touch alone
- 🔦 Torch Flash Alerts: Camera flash activates on cry detection for visual alerting
- 📊 Analytics Dashboard: View all cry events with time and type filtering. Includes date/time of cry, reason behind cry, classification confidence and temperature and humidity readings at time of cry.
- 📈 Data Visualisation: Bar charts for cry type distribution and time-of-day patterns
- 🌡️ Environment Monitoring: Live temperature and humidity readings with safety evaluations, helping parents maintain optimal room conditions for their baby's comfort and health
- 🧠 Science Page: Mel-spectrograms, audio playback of cry types and dataset information, giving parents transparency into how EchoCare works
- 📋 Downloadable Reports: Export cry data for sharing with healthcare professionals
- 🔄 Polling Fallback: Catches missed UDP packets via API polling every 2 seconds
- ⏱️ Automatic Pi Time Sync: Syncs the Pi's clock from the phone on each dashboard load
- 🏠 Fully Offline: No internet required, runs entirely on the local EchoCare network ensuring maximum privacy, security and sustainability

## 🛠️ Tech Stack
- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI**: XML Layouts, Material Design 3
- **Networking**: Retrofit (HTTP), UDP Socket (real-time)
- **Charts**: MPAndroidChart
- **Service**: Android Foreground Service
- **Communication**: UDP Broadcast Listener, REST API
- **Audio**: MediaPlayer (cry sample playback)
- **Export**: MediaStore (report download)

## 🔔 Notification System
EchoCare uses a multi-sensory alert system designed for deaf and hard-of-hearing parents:

- Push Notification: On-screen alert with cry type and confidence score
- Vibration Cry-type-specific patterns: rapid pulses (Hungry), long buzz (Pain), steady pulses (Normal)
- Torch Flash: Camera flashlight blinks 3 times on cry detection
- LED (on Pi): RGB LED flashes corresponding colour on the Raspberry Pi (Green = Hungry, Red = Pain, Blue = Normal)

## 🚀 Setup Instructions

### Prerequisites
- Android Studio
- Android phone (API 26+)
- EchoCare Backend running on Raspberry Pi: https://github.com/danellejp/echocare-infant-cry-classification

1) Clone EchoCare Frontend: https://github.com/danellejp/EchoCare-Android/
2) Open the project in Android Studio
3) Connect your Android phone via USB
4) Build and install: Run -> Run 'app'
5) Connect phone to the EchoCare_Network WiFi
6) Open the app -> Tap Start Monitoring -> Allow Notifications -> Tap Get Started