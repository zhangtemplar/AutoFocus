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

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

public class HelloOpenCvActivity extends Activity implements CvCameraViewListener2{
	private static final String  TAG = "Sample::Puzzle15::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    
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
	     mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
	     mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
	     mOpenCvCameraView.setCvCameraViewListener(this);
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
		try
		{
			List<Mat> rgb=new ArrayList<Mat>(3);
			Core.split(img, rgb);
			// convert the opponent color space
			List<Mat> opponent=rgb2opponent(rgb.get(0),rgb.get(1),rgb.get(2));
			// apply the FFT on each channel
			Mat sal=new Mat(img.rows(),img.cols(),CvType.CV_32F);;
			Core.add(imageSignature(opponent.get(0)), imageSignature(opponent.get(1)), sal);
			Core.add(imageSignature(opponent.get(2)), sal, sal);
			// apply the smooth and find the salient window
			// which can be achieved with a box filter
			Rect win=findSalientWindow(sal, new Size(64,64));
			// draw the results to the image
			Core.rectangle(img, win.tl(), win.br(), new Scalar(128,255,0));
		}
		catch (NullPointerException e)
		{
			Log.i(TAG, "error happens during saliency computation");
		}
		return img;
	}
	
	/**
	 * this function find the salient window from the image
	 * which is the window with maximal sum of salient score
	 * @param sal
	 * @return
	 */
	private Rect findSalientWindow(Mat sal, Size win_size)
	{
		Mat score=new Mat(sal.rows(),sal.cols(),sal.type());
		Imgproc.boxFilter(sal, score, -1, win_size);
		Core.MinMaxLocResult result=Core.minMaxLoc(score);
		return new Rect((int)(result.maxLoc.x-win_size.width/2),
				(int)(result.maxLoc.y-win_size.height/2),
				(int)win_size.width, (int)win_size.height);
	}
	
	/**
	 * this function converts rgb to opponent
	 * @param r
	 * @param g
	 * @param b
	 * @return
	 */
	private List<Mat> rgb2opponent(Mat r, Mat g, Mat b)
	{
		List<Mat> channel=new ArrayList<Mat>(3);
		Mat r2=new Mat(r.rows(),r.cols(),CvType.CV_32F);
		r.convertTo(r2, CvType.CV_32F);
		Mat g2=new Mat(r.rows(),r.cols(),CvType.CV_32F);
		g.convertTo(g2, CvType.CV_32F);
		Mat b2=new Mat(r.rows(),r.cols(),CvType.CV_32F);
		b.convertTo(b2, CvType.CV_32F);
		Mat y=new Mat(r.rows(),r.cols(),CvType.CV_32F);
		Core.add(r2,g2,y);
		Mat rg=new Mat(r.rows(),r.cols(),CvType.CV_32F);
		Core.subtract(r2, g2, rg);
		Mat rgb=new Mat(r.rows(),r.cols(),CvType.CV_32F);
		Core.subtract(b2,y,rgb);
		Core.add(r2,y,y);
		channel.add(y);
		channel.add(rg);
		channel.add(rgb);
		return channel;
	}
	
	/**
	 * this function compute the saliency map with image signature
	 * @param img
	 * @return
	 */
	private Mat imageSignature(Mat img)
	{
		Mat coef=new Mat(img.rows(),img.cols(),CvType.CV_32FC2);
		// forward dft
		Core.dft(img,coef,Core.DFT_COMPLEX_OUTPUT,img.rows());
		// unify the magnitude
		List<Mat> coef_complex=new ArrayList<Mat>(2);
		Core.split(coef,coef_complex);
		Mat mag=new Mat(img.rows(),img.cols(),CvType.CV_32F);
		Core.magnitude(coef_complex.get(0), coef_complex.get(1), mag);
		Core.divide(coef_complex.get(0),mag,coef_complex.get(0));
		Core.divide(coef_complex.get(1),mag,coef_complex.get(1));
		Core.merge(coef_complex, coef);
		// apply the inverse dft
		Mat sal=new Mat(img.rows(),img.cols(),CvType.CV_32FC2);
		Core.dft(coef,sal,Core.DCT_INVERSE,img.rows());
		// take square and magnitude
		Core.split(sal, coef_complex);
		Core.magnitude(coef_complex.get(0), coef_complex.get(1), mag);
		Core.multiply(mag, mag, mag);
		// perhaps smooth the image
		return mag;
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
}

