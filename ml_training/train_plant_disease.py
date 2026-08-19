import os
import sys
import json
import matplotlib.pyplot as plt
import tensorflow as tf
from tensorflow.keras import layers, models
from tensorflow.keras.applications import MobileNetV2

# Configuration
IMAGE_SIZE = (224, 224)
BATCH_SIZE = 32
EPOCHS = 15
LEARNING_RATE = 0.0001

def train_model(dataset_dir="dataset", output_dir="output"):
    """
    Train a MobileNetV2 Transfer Learning Model on Plant Disease Dataset
    and export as TensorFlow Lite (.tflite) for AgroAssist Android App.
    """
    if not os.path.exists(dataset_dir):
        print(f"ERROR: Dataset directory '{dataset_dir}' not found!")
        print("Please place your image dataset in a folder named 'dataset' with subfolders for each disease class.")
        return

    os.makedirs(output_dir, exist_ok=True)
    print(f"Loading dataset from '{dataset_dir}'...")

    # Load Training and Validation Sets
    train_ds = tf.keras.utils.image_dataset_from_directory(
        dataset_dir,
        validation_split=0.2,
        subset="training",
        seed=123,
        image_size=IMAGE_SIZE,
        batch_size=BATCH_SIZE
    )

    val_ds = tf.keras.utils.image_dataset_from_directory(
        dataset_dir,
        validation_split=0.2,
        subset="validation",
        seed=123,
        image_size=IMAGE_SIZE,
        batch_size=BATCH_SIZE
    )

    class_names = train_ds.class_names
    num_classes = len(class_names)
    print(f"\nFound {num_classes} plant disease classes:")
    for idx, name in enumerate(class_names):
        print(f" [{idx}] {name}")

    # Save class names label file
    labels_file = os.path.join(output_dir, "labels.txt")
    with open(labels_file, "w") as f:
        for name in class_names:
            f.write(f"{name}\n")
    print(f"\nSaved class labels to '{labels_file}'")

    # Data Augmentation & Normalization
    data_augmentation = tf.keras.Sequential([
        layers.RandomFlip("horizontal_and_vertical"),
        layers.RandomRotation(0.2),
        layers.RandomZoom(0.2),
        layers.RandomContrast(0.2)
    ])

    # Base Transfer Learning Model (MobileNetV2)
    base_model = MobileNetV2(
        input_shape=(224, 224, 3),
        include_top=False,
        weights="imagenet"
    )
    base_model.trainable = False

    # Build Architecture
    inputs = tf.keras.Input(shape=(224, 224, 3))
    x = tf.keras.applications.mobilenet_v2.preprocess_input(inputs)
    x = data_augmentation(x)
    x = base_model(x, training=False)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dropout(0.3)(x)
    outputs = layers.Dense(num_classes, activation="softmax")(x)

    model = tf.keras.Model(inputs, outputs)

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=LEARNING_RATE),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"]
    )

    model.summary()

    print("\nStarting Model Training...")
    history = model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=EPOCHS
    )

    # Save Keras Model
    keras_model_path = os.path.join(output_dir, "plant_disease_model.h5")
    model.save(keras_model_path)
    print(f"\nModel saved to '{keras_model_path}'")

    # Convert to TFLite (Optimized for Android)
    print("\nConverting model to TensorFlow Lite (.tflite) for AgroAssist App...")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]
    tflite_model = converter.convert()

    tflite_model_path = os.path.join(output_dir, "plant_disease_model.tflite")
    with open(tflite_model_path, "wb") as f:
        f.write(tflite_model)

    print(f"\n=======================================================")
    print(f" SUCCESS! TFLite model generated successfully!")
    print(f" TFLite Model File: {tflite_model_path}")
    print(f" Labels File:       {labels_file}")
    print(f"=======================================================")

if __name__ == "__main__":
    dataset_path = sys.argv[1] if len(sys.argv) > 1 else "dataset"
    train_model(dataset_path)
