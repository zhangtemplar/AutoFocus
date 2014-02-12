package com.qiang.autofocus;

import java.util.ArrayList;
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
	private static final String  TAG = "Sample::Puzzle15::Activity";

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

                    /* Now enable camera view to start receiving frames */
                    // mOpenCvCameraView.setOnTouchListener(HelloOpenCvActivity.this);
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
	     /**
	      * initialize
	      */
	     row=240;
	     col=320;
	     win_size=new Size(32,32);
	     
	     mImageSignature=new ImageSignature(row, col);
	     
	     win=new Rect();
	     
	     mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
	     mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
	     mOpenCvCameraView.setCvCameraViewListener(this);
	     mOpenCvCameraView.setMaxFrameSize(col, row);
	     
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

/**
 * this class implements the image signature for computing a saliency map from a given image
 * @author qzhang53
 *
 */
class ImageSignature
{
    // the size of frame
    private int row, col;
    // the saliency map
    private Mat sal;
    // for finding the most salient region
    private Mat score;
    // the most salient window or the focus window
    // private Rect win;
    // the splited RGB
    private List<Mat> rgb;
    // the connverted opponent
    private List<Mat> opponent;
    // the fouerier coeffcient
    private Mat coef;
    private List<Mat> coef_complex;
    private Mat mag;
    private Mat signature;
	
    /**
     * constructor which takes the size of image (r,c) as input
     * @param r
     * @param c
     */
	public ImageSignature(int r, int c)
	{
		row=r;
		col=c;
		sal=new Mat(row, col, CvType.CV_32FC1);
		score=new Mat(row, col, CvType.CV_32FC1);
		rgb=new ArrayList<Mat>(3);
		opponent=new ArrayList<Mat>(6);
		/**
		 * the opponent is a list of mat, where the first three elements 
		 * are the image in opponent color space, and the remaining three
		 * are some temporary result
		 */
     	opponent.add(new Mat(row, col, CvType.CV_32FC1));
     	opponent.add(new Mat(row, col, CvType.CV_32FC1));
     	opponent.add(new Mat(row, col, CvType.CV_32FC1));
     	opponent.add(new Mat(row, col, CvType.CV_32FC1));
     	opponent.add(new Mat(row, col, CvType.CV_32FC1));
     	opponent.add(new Mat(row, col, CvType.CV_32FC1));
		coef=new Mat(row, col, CvType.CV_32FC2);
		coef_complex=new ArrayList<Mat>(2);
		mag=new Mat(row, col,CvType.CV_32F);
		signature=new Mat(row, col,CvType.CV_32FC2);		
	}
	
	/**
	 * compute the saliency map for the given image
	 * @param img
	 */
	public void computeSaliency(Mat img)
	{
		assert(img.rows()==row && img.cols()==col);
		
		Core.split(img, rgb);
		// convert the opponent color space
		rgb2opponent(rgb.get(0),rgb.get(1),rgb.get(2), opponent);
		// apply the image signature on each channel
		imageSignature(opponent.get(0),mag,coef,coef_complex,signature);
		Core.add(sal,mag,sal);
		imageSignature(opponent.get(1),mag,coef,coef_complex,signature);
		Core.add(sal,mag,sal);
		imageSignature(opponent.get(2),mag,coef,coef_complex,signature);
		Core.add(sal,mag,sal);		
	}
	
	/**
	 * get the saliency map, we will (deep) copy the saliency mat to the provided parameter
	 * @param sal
	 */
	public void getSaliencyMap(Mat sal)
	{
		this.sal.copyTo(sal);
	}
	
	/**
	 * get the most saliency window
	 * @param img	the rgb image
	 * @param win_size	the size of window
	 * @return win	the window we found
	 */
	public void getSaliencyObject(Size win_size, Rect win)
	{
		// apply the smooth and find the salient window
		// which can be achieved with a box filter
		findSalientWindow(sal, win_size, win, score);
	}
	
	/**
	 * this function find the salient window from the image
	 * which is the window with maximal sum of salient score
	 * @param sal
	 * @return
	 */
	private void findSalientWindow(Mat sal, Size win_size, Rect win, Mat score)
	{
		Imgproc.boxFilter(sal, score, -1, win_size);
		Core.MinMaxLocResult result=Core.minMaxLoc(score);
		win.x=(int)(result.maxLoc.x-win_size.width/2);
		win.y=(int)(result.maxLoc.y-win_size.height/2);
		win.width=(int)win_size.width;
		win.height=(int)win_size.height;
	}
	
	/**
	 * this function converts rgb to opponent
	 * @param r
	 * @param g
	 * @param b
	 * @return opponent
	 */
	private void rgb2opponent(Mat r, Mat g, Mat b, List<Mat> opponent)
	{	
		// converts to float type
		r.convertTo(opponent.get(3), CvType.CV_32F);
		g.convertTo(opponent.get(4), CvType.CV_32F);
		b.convertTo(opponent.get(5), CvType.CV_32F);
		
		// convert the color space
		Core.add(opponent.get(3),opponent.get(4),opponent.get(0));
		Core.subtract(opponent.get(3), opponent.get(4), opponent.get(1));
		Core.subtract(opponent.get(5),opponent.get(0),opponent.get(2));
		Core.add(opponent.get(3),opponent.get(0),opponent.get(0));
	}
	
	/**
	 * this function compute the saliency map with image signature
	 * @param img
	 * @return
	 */
	private void imageSignature(Mat img, Mat mag, Mat coef, List<Mat> coef_complex, Mat signature)
	{
		// forward dft
		Core.dft(img,coef,Core.DFT_COMPLEX_OUTPUT,img.rows());
		// unify the magnitude
		
		Core.split(coef,coef_complex);
		
		Core.magnitude(coef_complex.get(0), coef_complex.get(1), mag);
		Core.divide(coef_complex.get(0),mag,coef_complex.get(0));
		Core.divide(coef_complex.get(1),mag,coef_complex.get(1));
		Core.merge(coef_complex, coef);
		// apply the inverse dft
		
		Core.dft(coef,signature,Core.DCT_INVERSE,img.rows());
		// take square and magnitude
		Core.split(signature, coef_complex);
		Core.magnitude(coef_complex.get(0), coef_complex.get(1), mag);
		Core.multiply(mag, mag, mag);
	}
}