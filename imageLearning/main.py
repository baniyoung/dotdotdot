import tensorflow as tf
import matplotlib.pyplot as plt

img_height, img_width=200,200
batch_size = 128
train_ds = tf.keras.utils.image_dataset_from_directory(
    "C:\pythonPrj\selenium\image\\train",
    image_size=(img_height, img_width),
    batch_size=batch_size)
val_ds = tf.keras.utils.image_dataset_from_directory(
    "C:\pythonPrj\selenium\image\\validation",
    image_size=(img_height, img_width),
    batch_size=batch_size)
test_ds = tf.keras.utils.image_dataset_from_directory(
    "C:\pythonPrj\selenium\image\\test",
    image_size=(img_height, img_width),
    batch_size=batch_size)

class_names=["2%","MD","TOPblack","TOPmaster","bita500","cider","cocacola","cozero","fanta","gatorade","hot6","letsbe","mccol","park","pear","pocari","powerade","sprite","welchs"]
plt.figure(figsize=(10,10))
for images, labels in train_ds.take(1):
    for i in range(5):
        ax = plt.subplot(5, 5, i + 1)
        plt.imshow(images[i].numpy().astype("uint8"))
        plt.title(class_names[labels[i]])
        plt.axis("off")

model = tf.keras.Sequential(
    [
     tf.keras.layers.Rescaling(1./255),
     tf.keras.layers.Conv2D(32, 19, activation="relu"),
     tf.keras.layers.MaxPooling2D(),
     tf.keras.layers.Conv2D(32, 19, activation="relu"),
     tf.keras.layers.MaxPooling2D(),
     tf.keras.layers.Conv2D(32, 19, activation="relu"),
     tf.keras.layers.MaxPooling2D(),
     tf.keras.layers.Flatten(),
     tf.keras.layers.Dense(128, activation="relu"),
     tf.keras.layers.Dense(19)
    ]
)

model.compile(
    optimizer="adam",
    loss=tf.losses.SparseCategoricalCrossentropy(from_logits = True),
    metrics=['accuracy']
)

model.fit(
    train_ds,
    validation_data = val_ds,
    epochs = 50
)

model.evaluate(test_ds)

import numpy

plt.figure(figsize=(10,10))
for images, labels in test_ds.take(1):
    classifications = model(images)
    #print(classifications)

    for i in range(9):
        ax=plt.subplot(3,3,i+1)
        plt.imshow(images[i].numpy().astype("uint8"))
        index=numpy.argmax(classifications[i])
        plt.title("Pred: " + class_names[index]+ " | Real" + class_names[labels[i]])


model.save("dotdotdot.h5")