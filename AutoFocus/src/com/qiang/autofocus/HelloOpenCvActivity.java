package com.qiang.autofocus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class HelloOpenCvActivity extends Activity implements CvCameraViewListener2, OnTouchListener{
	private static final String  TAG = "Qiang::AutoFocus::Activity";

    private AutoFocusView mOpenCvCameraView;
    
    // the saliency detector
    private ImageSignature mImageSignature;
    
    // size of image
    private int row, col;
    
    // size of salient object
    private Size win_size;
    
    // location of salient object;
    private Rect win;
    
    private List<Camera.Size> preview_size;
    private List<Size> focus_size;
    
    private MenuItem[] mPreviewMenuItems;
    private SubMenu mPreviewSizeMenu;
    private MenuItem[] mFocusMenuItems;
    private SubMenu mFocusSizeMenu;
    
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
                    preview_size=mOpenCvCameraView.getPreviewSize();
                    
                    focus_size = new ArrayList<Size>();
                    focus_size.add(new Size(16, 16));
                    focus_size.add(new Size(24, 24));
                    focus_size.add(new Size(32, 32));
                    focus_size.add(new Size(48, 48));
                    focus_size.add(new Size(64, 64));
                    focus_size.add(new Size(96, 96));
                    focus_size.add(new Size(128, 128));
	           	    win_size=focus_size.get(2);
	           	     
	           	    win=new Rect();
                    
                    setPreviewSize(preview_size.get(0));
                    
                    // mOpenCvCameraView.enableView();

                    /* Now enable camera view to start receiving frames */
                    mOpenCvCameraView.setOnTouchListener(HelloOpenCvActivity.this);
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
		 
		mOpenCvCameraView = (AutoFocusView) findViewById(R.id.HelloOpenCvView);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
		
	     
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
		// Core.flip(img, img, -1);
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
	 * response to the touch event
	 * on touch, we do the following three work
	 * 	change the focus window
	 * 	save the current image and the focus location
	 */
	@SuppressLint("SimpleDateFormat")
	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		Log.i(TAG,"onTouch event");
		// we may need to the event type
		
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	    String currentDateandTime = sdf.format(new Date());
	    String fileName = Environment.getExternalStorageDirectory().getPath() +
	                           "/sample_picture_" + currentDateandTime;
	    Rect rect=new Rect((int)arg1.getX(), (int)arg1.getY(), (int)win_size.width, (int)win_size.height);
	    // mOpenCvCameraView.touchFocus(rect, col, row);
	    mOpenCvCameraView.takePicture(fileName, rect);
	    Toast.makeText(this, fileName + " saved", Toast.LENGTH_SHORT).show();
		return false;
	}
	
	/**
	 * create the menu for preview size and focus size
	 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        mPreviewSizeMenu = menu.addSubMenu("Preview Size");
        mPreviewMenuItems = new MenuItem[preview_size.size()];

        int idx = 0;
        for (Camera.Size sz: preview_size)
        {
        	String element = new String(sz.width+"x"+sz.height);
            mPreviewMenuItems[idx] = mPreviewSizeMenu.add(1, idx, Menu.NONE, element);
            idx++;
        }

        mFocusSizeMenu = menu.addSubMenu("Focus Size");
        mFocusMenuItems = new MenuItem[focus_size.size()];

        idx = 0;
        for (Size sz: focus_size)
        {
        	mFocusMenuItems[idx] = mFocusSizeMenu.add(2, idx, Menu.NONE,
                    Integer.valueOf((int) sz.width).toString() + "x" + Integer.valueOf((int) sz.height).toString());
            idx++;
        }

        return true;
    }
    
    /**
     * when an menu item is selected
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        int id = item.getItemId();
        if (item.getGroupId() == 1)
        {
            setPreviewSize(preview_size.get(id));
            Toast.makeText(this, new String(preview_size.get(id).width+"x"+preview_size.get(id).height), Toast.LENGTH_SHORT).show();
        }
        else if (item.getGroupId() == 2)
        {
        	win_size=focus_size.get(id);
            Toast.makeText(this, new String(win_size.width+"x"+win_size.height), Toast.LENGTH_SHORT).show();
        }

        return true;
    }
    
    /**
     * update the preview size
     * when need to create a new image signature component
     * @param sz
     */
    public void setPreviewSize(Camera.Size sz)
    {
    	if (mOpenCvCameraView != null)
    	{
	        mOpenCvCameraView.disableView();
    	}
    	
    	row=sz.height;
        col=sz.width;
        mImageSignature=new ImageSignature(row, col);
        mOpenCvCameraView.setMaxFrameSize(col, row);
        
    	mOpenCvCameraView.enableView();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      // ignore orientation/keyboard change
      super.onConfigurationChanged(newConfig);
    }
}