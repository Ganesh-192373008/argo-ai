# AgroAssist Plant Disease Model Training Guide

This guide helps you train a custom AI model using your own plant disease image dataset and export it as an optimized **TensorFlow Lite (`plant_disease_model.tflite`)** file to run inside the **AgroAssist Android Application**.

---

## 📁 Step 1: Organize Your Dataset Folder

Create a folder named `dataset` inside `ml_training/` with subfolders for each crop/disease class:

```text
ml_training/
└── dataset/
    ├── Apple___Apple_scab/
    │   ├── img1.jpg
    │   └── img2.jpg
    ├── Apple___healthy/
    ├── Potato___Early_blight/
    ├── Potato___Late_blight/
    ├── Potato___healthy/
    ├── Tomato___Bacterial_spot/
    ├── Tomato___Early_blight/
    └── Tomato___healthy/
```

> 💡 **Tip**: Make sure each subfolder contains JPG or PNG image files of crop leaves.

---

## 🚀 Step 2: Run Training Script

### Option A: Local PC (Python + TensorFlow)

1. Open PowerShell or Terminal in the project root:
   ```bash
   pip install tensorflow matplotlib
   ```

2. Run the training script:
   ```bash
   python ml_training/train_plant_disease.py ml_training/dataset
   ```

3. The script will automatically output:
   * `ml_training/output/plant_disease_model.tflite`
   * `ml_training/output/labels.txt`

---

### Option B: Google Colab (Free GPU Training)

If you don't have a GPU on your local PC:
1. Zip your `dataset/` folder -> `dataset.zip`.
2. Open [Google Colab](https://colab.research.google.com/).
3. Upload `train_plant_disease.py` and `dataset.zip`.
4. Run:
   ```python
   !unzip dataset.zip
   !python train_plant_disease.py dataset
   ```
5. Download the generated `plant_disease_model.tflite` and `labels.txt`.

---

## 📱 Step 3: Integrate into AgroAssist Android App

Once training is complete:

1. Copy `plant_disease_model.tflite` and `labels.txt` into:
   `app/src/main/assets/`
2. Re-build your app (`.\gradlew.bat assembleDebug`).
3. The AgroAssist Detection screen (`DetectionActivity.kt`) will automatically load your custom AI model for offline leaf disease recognition!
