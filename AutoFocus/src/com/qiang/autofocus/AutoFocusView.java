/**
 * 
 */
package com.qiang.autofocus;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opencv.android.JavaCameraView;
import org.opencv.core.Rect;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;

/**
 * @author qzhang53
 * this class creates the view for the camera
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class AutoFocusView extends JavaCameraView implements PictureCallback {
	private static final String TAG="Qiang:AutoFocus:AutoFocusView";
	private String mPictureFileName;
	
	public AutoFocusView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * get the list of available preview size
	 * @return
	 */
	public List<Camera.Size> getPreviewSize()
	{
		boolean camera_closed=mCamera==null;
		if (camera_closed)
		{
			mCamera=Camera.open();
		}
		mCamera.setDisplayOrientation(180);
		Camera.Parameters params=mCamera.getParameters();
        List<Camera.Size> preview_size=params.getSupportedPreviewSizes();
        Collections.sort(preview_size, new CameraSizeComparator());
        if (camera_closed)
        {
        	mCamera.release();
        }
        return preview_size;
	}
	
	/**
	  * function for manually select the focus region
	  * @param tfocusRect
	  */
	public void touchFocus(final Rect tfocusRect, int col, int row)
	{
		mCamera.stopFaceDetection();
		mCamera.cancelAutoFocus();
		
		//Convert from View's width and height to +/- 1000
		final android.graphics.Rect targetFocusRect = new android.graphics.Rect(
			tfocusRect.x * 2000/col - 1000,
			tfocusRect.y * 2000/row - 1000,
			(tfocusRect.x+tfocusRect.width) * 2000/row - 1000,
			(tfocusRect.y+tfocusRect.height) * 2000/col - 1000);
		
		final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
		Camera.Area focusArea = new Camera.Area(targetFocusRect, 1000);
		focusList.add(focusArea);
		
		Parameters para = mCamera.getParameters();
		para.setFocusAreas(focusList);
		para.setMeteringAreas(focusList);
		mCamera.setParameters(para);
	}

    public void takePicture(final String fileName, Rect location) {
        Log.i(TAG, "Taking picture");
        this.mPictureFileName = fileName+"_"+location.x+"_"+location.y+"_"+location.width+"_"+location.height+".jpg";
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving a bitmap to file");
        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        // Write the image in a file (in jpeg format)
        try {
            FileOutputStream fos = new FileOutputStream(mPictureFileName);

            fos.write(data);
            fos.close();

        } catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }

    }
	 
}

class CameraSizeComparator implements Comparator<Camera.Size>
{
	public int compare(Camera.Size a, Camera.Size b)
	{
		return a.width-b.width;
	}
}