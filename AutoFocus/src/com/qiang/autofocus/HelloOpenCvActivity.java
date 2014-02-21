package com.qiang.autofocus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.JavaCameraView;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class HelloOpenCvActivity extends Activity implements CvCameraViewListener2{
	private static final String  TAG = "Qiang::AutoFocus::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    
    // for control the hardware camera, e.g., focus
    private Camera camera;
    
    // the saliency detector
    private ImageSignature mImageSignature;
    
    // size of image
    private int row, col;
    
    // size of salient object
    private Size win_size;
    
    // location of salient object;
    private Rect win;
    
    /**
     * those parameters are for computing the saliency map and check the focus region
     * we create those variables during class initialization to remove the overhead of
     * creating them everytime we get a frame
     */
    
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    
	           	    /**
	           	     * initialize
	           	     */
                    // for the preview size, we need to query the hardware for the list of available size
                    Camera.Parameters params=camera.getParameters();
                    List<Camera.Size> preview_size=params.getSupportedPreviewSizes();
                    Collections.sort(preview_size, new CameraSizeComparator());
                    row=preview_size.get(0).height;
                    col=preview_size.get(0).width;
                    camera.release();
                    
	           	    /*row=240;
	           	    col=320;*/
	           	    win_size=new Size(32,32);
	           	     
	           	    mImageSignature=new ImageSignature(row, col);
	           	     
	           	    win=new Rect();

                    /* Now enable camera view to start receiving frames */
                    // mOpenCvCameraView.setOnTouchListener(HelloOpenCvActivity.this);
	        		mOpenCvCameraView.setMaxFrameSize(col, row);
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.helloopencvlayout);
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.helloopencvlayout);
		 
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
		
		camera=Camera.open();
		
	     
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.helloopencvlayout, menu);
		return true;
	}

	@Override
	public void onResume()
	{
	    super.onResume();
	    OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		// TODO Auto-generated method stub
		// add the saliency code here
		// grab a frame
		Mat img=inputFrame.rgba();
		// the image is up-down flipped
		Core.flip(img, img, -1);
		// compute the saliency map
		mImageSignature.computeSaliency(img);
		mImageSignature.getSaliencyObject(win_size, win);
		// draw the results to the image
		Core.rectangle(img, win.tl(), win.br(), new Scalar(128,255,0));
		return img;
	}


	@Override
	public void onCameraViewStarted(int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCameraViewStopped() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	 public void onPause()
	 {
	     super.onPause();
	     if (mOpenCvCameraView != null)
	         mOpenCvCameraView.disableView();
	 }

	 public void onDestroy() {
	     super.onDestroy();
	     if (mOpenCvCameraView != null)
	         mOpenCvCameraView.disableView();
	 }
	 
	 /**
	  * function for manually select the focus region
	  * @param tfocusRect
	  */
	 public void touchFocus(final Rect tfocusRect)
	 {
    	camera.stopFaceDetection();
		
    	//Convert from View's width and height to +/- 1000
		final android.graphics.Rect targetFocusRect = new android.graphics.Rect(
			tfocusRect.x * 2000/col - 1000,
			tfocusRect.y * 2000/row - 1000,
			(tfocusRect.x+tfocusRect.width) * 2000/row - 1000,
			(tfocusRect.y+tfocusRect.height) * 2000/col - 1000);
		
		final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
		Camera.Area focusArea = new Camera.Area(targetFocusRect, 1000);
		focusList.add(focusArea);
		
		Parameters para = camera.getParameters();
		para.setFocusAreas(focusList);
		para.setMeteringAreas(focusList);
		camera.setParameters(para);
    }
}

class CameraSizeComparator implements Comparator<Camera.Size>
{
	public int compare(Camera.Size a, Camera.Size b)
	{
		return a.width-b.width;
	}
}