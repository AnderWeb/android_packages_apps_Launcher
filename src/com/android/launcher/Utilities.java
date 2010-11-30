/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher;

import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Canvas;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.content.res.Resources;
import android.content.Context;

/**
 * Various utilities shared amongst the Launcher's classes.
 */
final class Utilities {
    private static int sIconWidth = -1;
    private static int sIconHeight = -1;

    private static final Paint sPaint = new Paint();
    private static final Rect sBounds = new Rect();
    private static final Rect sOldBounds = new Rect();
    private static Canvas sCanvas = new Canvas();

    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
                Paint.FILTER_BITMAP_FLAG));
    }

    /**
     * Returns a Drawable representing the thumbnail of the specified Drawable.
     * The size of the thumbnail is defined by the dimension
     * android.R.dimen.launcher_application_icon_size.
     *
     * This method is not thread-safe and should be invoked on the UI thread only.
     *
     * @param icon The icon to get a thumbnail of.
     * @param context The application's context.
     *
     * @return A thumbnail for the specified icon or the icon itself if the
     *         thumbnail could not be created. 
     */
    static Drawable createIconThumbnail(Drawable icon, Context context) {
        if (sIconWidth == -1) {
            final Resources resources = context.getResources();
            sIconWidth = sIconHeight = (int) resources.getDimension(android.R.dimen.app_icon_size);
        }

        int width = sIconWidth;
        int height = sIconHeight;

        float scale = 1.0f;
        if (icon instanceof PaintDrawable) {
            PaintDrawable painter = (PaintDrawable) icon;
            painter.setIntrinsicWidth(width);
            painter.setIntrinsicHeight(height);
        } else if (icon instanceof BitmapDrawable) {
            // Ensure the bitmap has a density.
            BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
            }
        }
        int iconWidth = icon.getIntrinsicWidth();
        int iconHeight = icon.getIntrinsicHeight();

        if (width > 0 && height > 0) {
            if (width < iconWidth || height < iconHeight || scale != 1.0f) {
                final float ratio = (float) iconWidth / iconHeight;

                if (iconWidth > iconHeight) {
                    height = (int) (width / ratio);
                } else if (iconHeight > iconWidth) {
                    width = (int) (height * ratio);
                }

                final Bitmap.Config c = icon.getOpacity() != PixelFormat.OPAQUE ?
                            Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
                final Bitmap thumb = Bitmap.createBitmap(sIconWidth, sIconHeight, c);
                final Canvas canvas = sCanvas;
                canvas.setBitmap(thumb);
                // Copy the old bounds to restore them later
                // If we were to do oldBounds = icon.getBounds(),
                // the call to setBounds() that follows would
                // change the same instance and we would lose the
                // old bounds
                sOldBounds.set(icon.getBounds());
                final int x = (sIconWidth - width) / 2;
                final int y = (sIconHeight - height) / 2;
                icon.setBounds(x, y, x + width, y + height);
                icon.draw(canvas);
                icon.setBounds(sOldBounds);
                icon = new FastBitmapDrawable(thumb);
            } else if (iconWidth < width && iconHeight < height) {
                final Bitmap.Config c = Bitmap.Config.ARGB_8888;
                final Bitmap thumb = Bitmap.createBitmap(sIconWidth, sIconHeight, c);
                final Canvas canvas = sCanvas;
                canvas.setBitmap(thumb);
                sOldBounds.set(icon.getBounds());
                final int x = (width - iconWidth) / 2;
                final int y = (height - iconHeight) / 2;
                icon.setBounds(x, y, x + iconWidth, y + iconHeight);
                icon.draw(canvas);
                icon.setBounds(sOldBounds);
                icon = new FastBitmapDrawable(thumb);
            }
        }

        return icon;
    }

    /**
     * Returns a Bitmap representing the thumbnail of the specified Bitmap.
     * The size of the thumbnail is defined by the dimension
     * android.R.dimen.launcher_application_icon_size.
     *
     * This method is not thread-safe and should be invoked on the UI thread only.
     *
     * @param bitmap The bitmap to get a thumbnail of.
     * @param context The application's context.
     *
     * @return A thumbnail for the specified bitmap or the bitmap itself if the
     *         thumbnail could not be created.
     */
    static Bitmap createBitmapThumbnail(Bitmap bitmap, Context context) {
        if (sIconWidth == -1) {
            final Resources resources = context.getResources();
            sIconWidth = sIconHeight = (int) resources.getDimension(
                    android.R.dimen.app_icon_size);
        }

        int width = sIconWidth;
        int height = sIconHeight;

        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        if (width > 0 && height > 0) {
            if (width < bitmapWidth || height < bitmapHeight) {
                final float ratio = (float) bitmapWidth / bitmapHeight;
    
                if (bitmapWidth > bitmapHeight) {
                    height = (int) (width / ratio);
                } else if (bitmapHeight > bitmapWidth) {
                    width = (int) (height * ratio);
                }
    
                final Bitmap.Config c = (width == sIconWidth && height == sIconHeight && bitmap.getConfig()!=null) ?
                        bitmap.getConfig() : Bitmap.Config.ARGB_8888;
                final Bitmap thumb = Bitmap.createBitmap(sIconWidth, sIconHeight, c);
                final Canvas canvas = sCanvas;
                final Paint paint = sPaint;
                canvas.setBitmap(thumb);
                paint.setDither(false);
                paint.setFilterBitmap(true);
                sBounds.set((sIconWidth - width) / 2, (sIconHeight - height) / 2, width, height);
                sOldBounds.set(0, 0, bitmapWidth, bitmapHeight);
                canvas.drawBitmap(bitmap, sOldBounds, sBounds, paint);
                return thumb;
            } else if (bitmapWidth < width || bitmapHeight < height) {
                final Bitmap.Config c = Bitmap.Config.ARGB_8888;
                final Bitmap thumb = Bitmap.createBitmap(sIconWidth, sIconHeight, c);
                final Canvas canvas = sCanvas;
                final Paint paint = sPaint;
                canvas.setBitmap(thumb);
                paint.setDither(false);
                paint.setFilterBitmap(true);
                canvas.drawBitmap(bitmap, (sIconWidth - bitmapWidth) / 2,
                        (sIconHeight - bitmapHeight) / 2, paint);
                return thumb;
            }
        }

        return bitmap;
    }
    /**
     *  ADW Create an icon drawable with reflection :P
     *  Thanks to http://www.inter-fuser.com/2009/12/android-reflections-with-bitmaps.html
     * @param icon
     * @param context
     * @return
     */
    static Drawable drawReflection(Drawable icon,Context context){
    	final Resources resources=context.getResources();
    	sIconWidth = sIconHeight = (int) resources.getDimension(android.R.dimen.app_icon_size);
    	//The gap we want between the reflection and the original image
        final float scale=1.30f;
      
        int width = sIconWidth;
        int height = sIconHeight;
        float ratio=sIconHeight/(sIconHeight*scale);
        Bitmap original;
        try{
            original= Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            return icon;
        }
        final Canvas cv = new Canvas();
        cv.setBitmap(original);
        icon.setBounds(0,0, width, height);
        icon.draw(cv);
        //This will not scale but will flip on the Y axis
        Matrix matrix = new Matrix();
        matrix.preScale(1, -1);
        
        //Create a Bitmap with the flip matix applied to it.
        //We only want the bottom half of the image
        Bitmap reflectionImage;
        try{
            reflectionImage= Bitmap.createBitmap(original, 0, height/2, width, height/2, matrix, false);
        } catch (OutOfMemoryError e) {
            return new FastBitmapDrawable(original);
        }

        //Create a new bitmap with same width but taller to fit reflection
        Bitmap bitmapWithReflection;
        try{
            bitmapWithReflection= Bitmap.createBitmap(width 
          , (int) (height*scale), Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            return new FastBitmapDrawable(original);
        }
      
       //Create a new Canvas with the bitmap that's big enough for
       //the image plus gap plus reflection
       Canvas canvas = new Canvas(bitmapWithReflection);
       //Draw in the gap
       //Paint deafaultPaint = new Paint();
       //canvas.drawRect(0, height, width, height + reflectionGap, deafaultPaint);
       //Draw in the reflection
       canvas.drawBitmap(reflectionImage,0, height-6, null);
       
       //Create a shader that is a linear gradient that covers the reflection
       Paint paint = new Paint(); 
       LinearGradient shader = new LinearGradient(0, original.getHeight(), 0, 
         bitmapWithReflection.getHeight(), 0x70ffffff, 0x00ffffff, 
         TileMode.CLAMP); 
       //Set the paint to use this shader (linear gradient)
       paint.setShader(shader); 
       //Set the Transfer mode to be porter duff and destination in
       paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN)); 
       //Draw a rectangle using the paint with our linear gradient
       canvas.drawRect(0, height-6, width, 
         bitmapWithReflection.getHeight(), paint); 
       //Draw in the original image
       canvas.drawBitmap(original, 0, 0, null);
       original.recycle();
       reflectionImage.recycle();
       try{
           return new FastBitmapDrawable(Bitmap.createScaledBitmap(bitmapWithReflection,Math.round((float)sIconWidth*ratio),sIconHeight,true));
       }catch(OutOfMemoryError e){
           return icon;
       }
    }
    /**
     *  ADW Create an icon drawable scaled
     *  Used for Action Buttons
     * @param icon
     * @param context
     * @param tint
     * @return
     */
    static Drawable scaledDrawable(Drawable icon,Context context, boolean tint, float scale, int color){
        final Resources resources=context.getResources();
    	sIconWidth = sIconHeight = (int) resources.getDimension(android.R.dimen.app_icon_size);
      
        int width = sIconWidth;
        int height = sIconHeight;
        Bitmap original;
        try{
             original= Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            return icon;
        }
        Canvas canvas = new Canvas(original);
        canvas.setBitmap(original);
        icon.setBounds(0,0, width, height);
        icon.draw(canvas);
        
        if(tint){
	        Paint paint = new Paint(); 
	        LinearGradient shader = new LinearGradient(width/2, 0, width/2, 
	          height,
	          Color.argb(220, Color.red(color), Color.green(color), Color.blue(color)),
	          Color.argb(50, Color.red(color), Color.green(color), Color.blue(color)),
	          TileMode.CLAMP); 
	        paint.setShader(shader); 
	        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	        canvas.drawRect(0, 0, width, 
	                height, paint);
        }
        try{
        	Bitmap endImage=Bitmap.createScaledBitmap(original, (int)(width*scale), (int)(height*scale), true);
        	original.recycle();
        	return new FastBitmapDrawable(endImage);
        } catch (OutOfMemoryError e) {
            return icon;
        }
    }
    /**
     * ADW: Use donut syule wallpaper rendering, we need this method to fit wallpaper bitmap
     */
    static Bitmap centerToFit(Bitmap bitmap, int width, int height, Context context) {
        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        if (bitmapWidth < width || bitmapHeight < height) {
            int color = context.getResources().getColor(R.color.window_background);

            Bitmap centered = Bitmap.createBitmap(bitmapWidth < width ? width : bitmapWidth,
                    bitmapHeight < height ? height : bitmapHeight, Bitmap.Config.RGB_565);
            centered.setDensity(bitmap.getDensity());
            Canvas canvas = new Canvas(centered);
            canvas.drawColor(color);
            canvas.drawBitmap(bitmap, (width - bitmapWidth) / 2.0f, (height - bitmapHeight) / 2.0f,
                    null);

            bitmap = centered;
        }

        return bitmap;
    }
}
