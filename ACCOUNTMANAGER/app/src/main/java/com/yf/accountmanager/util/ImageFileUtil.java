package com.yf.accountmanager.util;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.yf.accountmanager.R;
import com.yf.accountmanager.common.FileCache;
import com.yf.accountmanager.common.IConstants;
import com.yf.accountmanager.common.MemoryCache;

public class ImageFileUtil {
	public static DisplayMetrics dm;
	static {
		dm = DeviceInfoUtil.getDisplayMetrics();
	}

	private static FileCache fileCache = null;/* FileCache.getInstance(); */

	private static MemoryCache memoryCache = MemoryCache.getInstance();

	private static Map<ImageView, String> map = Collections
			.synchronizedMap(new WeakHashMap<ImageView, String>());

	private static Map<String, ImageView> loadingUrls = new HashMap<String, ImageView>();

	private static Handler handler = IConstants.MAIN_HANDLER;

	private static final int dimen_45 = CommonUtils.context.getResources()
			.getDimensionPixelOffset(R.dimen.dp_45);

	public static void prepareLoadImage(ImageView imageView, String url) {
		map.put(imageView, url);
	}

	private static Bitmap getBitmap(String url) throws Exception {
		Bitmap bm = null;
		if (fileCache != null) {
			bm = fileCache.getBitmap(url);
			if (bm != null) {
				return bm;
			}
		}
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		bm = BitmapFactory.decodeFile(url, opts);
		int sampleSize = (opts.outWidth > opts.outHeight ? opts.outHeight
				: opts.outWidth) / dimen_45;
		// System.out.println(sampleSize+",outWidth="+opts.outWidth+",outHeight="+opts.outHeight+","+dimen_45+"     @ImageFileUtil");
		opts.inJustDecodeBounds = false;
		// opts.inMutable=true;
		opts.inSampleSize = sampleSize;
		bm = BitmapFactory.decodeFile(url, opts);
		// System.out.println(sampleSize+",outWidth="+opts.outWidth+",outHeight="+opts.outHeight+","+bm.getRowBytes()+"     @ImageFileUtil");
		return bm;

	}

	public static void asyncLoadImageAndShow(ImageView imageView, File file,
			Context context, boolean busy) {
		if (imageView == null || file == null)
			return;
		// TODO
		final String url = file.getAbsolutePath();
		// System.out.println(url+"\n"+imageView+"  before getfromCache  +++   @@@@ImageFileUtil");
		Bitmap bitmap = memoryCache.getBitmap(url);
		if (bitmap != null) {
			imageView.setImageBitmap(bitmap);
			return;
		}
		if (busy)
			return;
		// prepareLoadImage(imageView, url);

		if (loadingUrls.containsKey(url)) {
			loadingUrls.put(url, imageView);
			return;
		}
		loadingUrls.put(url, imageView);
		IConstants.THREAD_POOL.submit(new Runnable() {
			public void run() {
				try {
					if (posChanged(loadingUrls.get(url), url)){
						loadingUrls.remove(url);
						return;
					}

					final Bitmap bm = getBitmap(url);
					if (bm != null)
						memoryCache.cacheBitmap(url, bm);
					if (posChanged(loadingUrls.get(url), url)){
						loadingUrls.remove(url);
						return;
					}

					handler.post(new Runnable() {
						public void run() {
							if (!posChanged(loadingUrls.get(url), url)) {

								if (bm == null) {
									// failure to load image
									loadingUrls.get(url).setImageResource(
											R.drawable.icon_img_decfailure);
								} else {
									loadingUrls.get(url).setImageBitmap(bm);
								}
							}
							loadingUrls.remove(url);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					// load image exception
					handler.post(new Runnable() {
						public void run() {
							if (!posChanged(loadingUrls.get(url), url)) {
								loadingUrls.get(url).setImageResource(
										R.drawable.icon_imgdecerror);
							}
							loadingUrls.remove(url);
						}
					});
				} finally {
					// TODO
				}

			}
		});
	}

	private static Bitmap resizeBitmap(Bitmap bitmap, ImageView container) {
		Matrix matrix = new Matrix();
		float fx = container.getWidth() / (float) bitmap.getWidth();
		matrix.setScale(fx, fx);
		Bitmap bm = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
				bitmap.getHeight(), matrix, true);
		if (bitmap != bm)
			bitmap.recycle();
		return bm;
	}

	private static boolean posChanged(ImageView view, String url) {
		return !url.equals(map.get(view));
	}

}
