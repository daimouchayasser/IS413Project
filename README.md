# MNIST Digit Classifier

Android application for recognizing handwritten digits using machine learning. This project was developed for IS 413 - GUI Systems Using Java.

## What It Does

This app lets you draw digits (0-9) on your phone screen and uses a TensorFlow Lite model to classify what digit you drew. It shows you the predicted digit and how confident the model is in its prediction.

## Features

- Draw digits with your finger on a touchscreen
- Real-time digit classification using TensorFlow Lite
- Shows prediction confidence scores
- Simple, clean interface
- Works offline (no internet required)

## Requirements

- Android Studio
- Android device or emulator running API 24 (Android 7.0) or higher
- The digit.tflite model file (already included in assets folder)

## Setup Instructions

1. Clone or download this repository
2. Open the project in Android Studio
3. Wait for Gradle to sync and download dependencies
4. Make sure the `digit.tflite` file is in `app/src/main/assets/`
5. Connect an Android device or start an emulator
6. Click Run to build and install the app

## How to Use

1. Launch the app
2. Draw a digit (0-9) on the black canvas using your finger
3. Tap the "Classify" button
4. The app will show the predicted digit and confidence percentage
5. Tap "Clear" to erase and draw another digit

## Project Structure

- `MainActivity.java` - Main application code with classification logic
- `activity_main.xml` - User interface layout
- `digit.tflite` - Pre-trained TensorFlow Lite model for digit recognition
- `build.gradle.kts` - Project dependencies and configuration

## Technologies Used

- Android SDK
- TensorFlow Lite for machine learning inference
- FingerPaintView library for drawing interface
- Java NIO for efficient file and memory operations

## Technical Details

The app follows the processing sequence from the course materials:
1. Loads the TensorFlow Lite model using MappedByteBuffer
2. Exports the drawing to a 28x28 bitmap
3. Converts pixel data to the format expected by the model
4. Runs inference using the TensorFlow Lite Interpreter
5. Displays the classification results

## Notes

- Draw digits large enough to fill most of the canvas for best results
- The model works best with clear, well-formed digits
- Classification happens locally on your device

## Course Information

This project was completed for IS 413 at UMBC, following the processing sequence and using the classes and techniques covered in the course materials.

## License

This project is for educational purposes as part of the IS 413 course.

