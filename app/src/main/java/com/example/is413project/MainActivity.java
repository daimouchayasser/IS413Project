package com.example.is413project;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.nex3z.fingerpaintview.FingerPaintView;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * MainActivity for the MNIST Digit Classifier application.
 * 
 * This activity implements a handwritten digit recognition system using
 * TensorFlow Lite. Users can draw digits on a FingerPaintView and receive
 * real-time classification results.
 * 
 * The implementation follows the processing sequence outlined in the course
 * materials, utilizing MappedByteBuffer for model loading, Bitmap for image
 * processing, and ByteBuffer for data conversion.
 * 
 * @author IS 413 Project Team
 * @version 1.0
 */
public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MNISTClassifier";
    private static final String MODEL_FILE = "digit.tflite";
    private static final int IMAGE_WIDTH = 28;
    private static final int IMAGE_HEIGHT = 28;
    private static final int NUM_CLASSES = 10;
    
    // UI components
    private FingerPaintView fingerPaintView;
    private TextView resultText;
    private Button classifyButton;
    private Button clearButton;
    
    // TensorFlow Lite interpreter
    private Interpreter tflite;
    
    /**
     * Called when the activity is first created.
     * Initializes the UI components and loads the TensorFlow Lite model.
     * 
     * @param savedInstanceState If the activity is being re-initialized after
     *                            previously being shut down, this Bundle contains
     *                            the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        setupViews();
        setupModel();
    }
    
    /**
     * Initializes and configures the UI components.
     * Sets up the FingerPaintView with appropriate drawing properties,
     * configures button click listeners, and initializes the result display.
     */
    private void setupViews() {
        // Find views by ID
        fingerPaintView = findViewById(R.id.finger_paint_view);
        resultText = findViewById(R.id.result_text);
        classifyButton = findViewById(R.id.classify_button);
        clearButton = findViewById(R.id.clear_button);
        
        // Configure FingerPaintView for optimal digit recognition
        // Black background matches MNIST dataset format
        fingerPaintView.setBackgroundColor(0xFF000000);
        // White paint for drawing digits
        fingerPaintView.setColor(0xFFFFFFFF);
        // Note: Stroke width setting removed - FingerPaintView library may not support
        // setStrokeWidth() or setPenWidth() methods. The default stroke width should work fine.
        
        // Set up button click listeners
        classifyButton.setOnClickListener(v -> classifyDigit());
        clearButton.setOnClickListener(v -> clearCanvas());
        
        // Initialize result text
        resultText.setText("Draw a digit (0-9) and press Classify");
    }
    
    /**
     * Loads the TensorFlow Lite model from the assets folder.
     * Uses MappedByteBuffer for efficient memory-mapped file access,
     * following the approach demonstrated in the course materials.
     */
    private void setupModel() {
        try {
            // Load model file into MappedByteBuffer
            MappedByteBuffer modelBuffer = loadModelFile();
            
            // Create TensorFlow Lite interpreter
            tflite = new Interpreter(modelBuffer);
            
            Log.d(TAG, "Model loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error loading model: " + e.getMessage(), e);
            resultText.setText("Error loading model: " + e.getMessage());
        }
    }
    
    /**
     * Loads the TensorFlow Lite model file from assets into a MappedByteBuffer.
     * 
     * This method follows the processing sequence from the course materials:
     * 1. Get AssetFileDescriptor from assets
     * 2. Create FileInputStream from the file descriptor
     * 3. Get FileChannel from the input stream
     * 4. Map the file to memory using FileChannel.map()
     * 
     * @param fileName The name of the model file in the assets folder
     * @return MappedByteBuffer containing the model data
     * @throws IOException If the file cannot be read
     */
    private MappedByteBuffer loadModelFile() throws IOException {
        // Get application context
        Context ctx = getApplicationContext();
        
        // Open the asset file and get file descriptor
        AssetFileDescriptor fd = ctx.getAssets().openFd(MODEL_FILE);
        
        Log.d(TAG, "File descriptor: " + fd.getFileDescriptor().toString());
        
        // Get the byte offset where the asset data starts
        long startOffset = fd.getStartOffset();
        Log.d(TAG, "Start offset: " + startOffset);
        
        // Get the declared length of the asset
        long declaredLength = fd.getDeclaredLength();
        Log.d(TAG, "Declared length: " + declaredLength);
        
        // Create FileInputStream from the file descriptor
        FileInputStream inputStream = new FileInputStream(fd.getFileDescriptor());
        
        // Create FileChannel from the FileInputStream
        FileChannel fileChannel = inputStream.getChannel();
        
        // Create memory-mapped byte buffer using the map method
        // READ_ONLY mode is sufficient for model inference
        MappedByteBuffer mbb = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            startOffset,
            declaredLength
        );
        
        // Return the mapped byte buffer
        return mbb;
    }
    
    /**
     * Classifies the digit drawn on the FingerPaintView.
     * 
     * This method implements the complete inference pipeline:
     * 1. Export drawing to Bitmap using FingerPaintView.exportToBitmap()
     * 2. Preprocess the bitmap to extract pixel data
     * 3. Convert pixels to ByteBuffer format expected by the model
     * 4. Run inference using Interpreter.run()
     * 5. Display the classification results
     */
    private void classifyDigit() {
        try {
            // Step 1: Export drawing to Bitmap
            // Use 28x28 dimensions to match MNIST dataset
            Bitmap bitmap = fingerPaintView.exportToBitmap(IMAGE_WIDTH, IMAGE_HEIGHT);
            
            if (bitmap == null) {
                Log.e(TAG, "Failed to export bitmap");
                resultText.setText("Error: Could not export drawing");
                return;
            }
            
            // Step 2: Convert bitmap to ByteBuffer
            ByteBuffer inputBuffer = convertBitmapToByteBuffer(bitmap);
            
            // Step 3: Prepare output array for model predictions
            // Output is a 2D array: [1][NUM_CLASSES]
            float[][] output = new float[1][NUM_CLASSES];
            
            // Step 4: Run inference
            tflite.run(inputBuffer, output);
            
            // Step 5: Process and display results
            displayResults(output[0]);
            
        } catch (Exception e) {
            Log.e(TAG, "Classification error: " + e.getMessage(), e);
            resultText.setText("Classification failed: " + e.getMessage());
        }
    }
    
    /**
     * Converts a Bitmap to a ByteBuffer suitable for TensorFlow Lite inference.
     * 
     * This method follows the processing sequence from the course materials:
     * 1. Extract pixels from Bitmap using getPixels()
     * 2. Process each pixel to convert ARGB to normalized float value
     * 3. Store values in ByteBuffer using putFloat()
     * 
     * @param bitmap The bitmap to convert
     * @return ByteBuffer containing normalized pixel values
     */
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        // Allocate direct byte buffer for better performance
        // Size: IMAGE_WIDTH * IMAGE_HEIGHT * 4 bytes (float size)
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(IMAGE_WIDTH * IMAGE_HEIGHT * 4);
        
        // Set byte order to native order for optimal performance
        inputBuffer.order(ByteOrder.nativeOrder());
        
        // Reset buffer position to beginning
        inputBuffer.rewind();
        
        // Array to hold pixel data
        // Size: width * height
        int[] pixels = new int[IMAGE_WIDTH * IMAGE_HEIGHT];
        
        // Extract pixels from bitmap
        // Parameters: pixels array, offset, stride, x, y, width, height
        bitmap.getPixels(pixels, 0, IMAGE_WIDTH, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        
        // Process each pixel
        for (int pixel : pixels) {
            // Preprocess pixel: convert ARGB to normalized float
            float normalizedPixel = preprocessPixel(pixel);
            
            // Add pixel value to ByteBuffer
            inputBuffer.putFloat(normalizedPixel);
        }
        
        return inputBuffer;
    }
    
    /**
     * Preprocesses a single pixel value from ARGB format to normalized float.
     * 
     * Following the processing sequence from the course materials:
     * - Each pixel is a 32-bit int in ARGB format
     * - For black and white images, we check if pixel is white (0xFFFFFFFF)
     * - White pixels (digit) should be 1.0, black pixels (background) should be 0.0
     * 
     * However, since FingerPaintView may produce anti-aliased pixels with
     * shades of gray, we use a threshold-based approach:
     * - Extract RGB components
     * - Convert to grayscale
     * - Normalize to [0, 1] range
     * - Invert so that white (digit) becomes 1.0 and black (background) becomes 0.0
     * 
     * @param pixel The ARGB pixel value
     * @return Normalized float value (0.0 for background, ~1.0 for digit)
     */
    private float preprocessPixel(int pixel) {
        // Extract RGB components from ARGB format
        // ARGB format: bits 31-24 = Alpha, 23-16 = Red, 15-8 = Green, 7-0 = Blue
        int r = (pixel >> 16) & 0xFF;  // Red component
        int g = (pixel >> 8) & 0xFF;   // Green component
        int b = pixel & 0xFF;           // Blue component
        
        // Convert to grayscale using standard luminance weights
        // These weights approximate human perception of brightness
        float grayscale = (r * 0.299f + g * 0.587f + b * 0.114f) / 255.0f;
        
        // Invert and normalize for MNIST format
        // MNIST expects: background = 0, digit = 1
        // Our bitmap: background = black (0), digit = white (1)
        // So we invert: 1.0 - grayscale
        // This ensures white pixels become 1.0 and black pixels become 0.0
        return 1.0f - grayscale;
    }
    
    /**
     * Displays the classification results to the user.
     * 
     * Finds the digit with the highest probability and displays both
     * the predicted digit and its confidence level. Also shows the
     * probability distribution for all classes for educational purposes.
     * 
     * @param probabilities Array of probabilities for each digit class (0-9)
     */
    private void displayResults(float[] probabilities) {
        // Find the digit with highest probability
        int maxIndex = 0;
        float maxProbability = probabilities[0];
        
        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > maxProbability) {
                maxProbability = probabilities[i];
                maxIndex = i;
            }
        }
        
        // Build result string with prediction and confidence
        String result = String.format(
            "Predicted: %d\nConfidence: %.1f%%",
            maxIndex,
            maxProbability * 100
        );
        
        // Display the result
        resultText.setText(result);
        
        // Log all probabilities for debugging
        StringBuilder logBuilder = new StringBuilder("Probabilities: ");
        for (int i = 0; i < probabilities.length; i++) {
            logBuilder.append(String.format("%d:%.2f ", i, probabilities[i]));
        }
        Log.d(TAG, logBuilder.toString());
    }
    
    /**
     * Clears the drawing canvas and resets the result display.
     */
    private void clearCanvas() {
        fingerPaintView.clear();
        resultText.setText("Draw a digit (0-9) and press Classify");
    }
    
    /**
     * Called when the activity is being destroyed.
     * Cleans up resources, particularly closing the TensorFlow Lite interpreter
     * to prevent memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Close the TensorFlow Lite interpreter to free resources
        if (tflite != null) {
            tflite.close();
            tflite = null;
            Log.d(TAG, "Interpreter closed");
        }
    }
}

