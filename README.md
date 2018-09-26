#Library to illustrate simple usage of face detection
- Library open camera view with an oval overlay at the center. Once you alignt the face to be inside the overlay, a selfie is taken and shown to the user.
- On clicking the CANCEL button, you come back to previous screen
- On click the center ROTATE CAMERA button, the camera flips from front to back and vice versa.
- On click of TICK button, if an image has been successfully capture it is passed as intent extra to the activity which called it as byteArray

#Bonus
- An eye patch is also attached to the face as an overlay to the result image.
- Emotion detected based on facial expression are shown as live feedback


To call the library use the below code

~~~~
TakeSelfie(context).start(REQUEST_CODE)
~~~~

To get the image back from the library
~~~~
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELFIE && resultCode == Activity.RESULT_OK) {
            data?.getByteArrayExtra("data")?.let {
                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            }
        }
    }
~~~~~


Sample app open with an activity with a button. Clicking on the button will open the library