package com.android.launcher;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

/**
 * Provides methods and constructs for handling themes with an icon shader.
 * Create parser from theme/res/xml/shader.xml
 * Compile with CompiledIconShader parseXml(XmlResourceParser xpp)
 * Process icons with Drawable processIcon(Drawable icon, CompiledIconShader c)
 */
class IconShader {
    
    static class IMAGE {
        static final int ICON = 0;
        static final int BUFFER = 1;
        static final int OUTPUT = 2;
    }

    static class MODE {
        static final int NONE = 0;
        static final int WRITE = 1;
        static final int MULTIPLY = 2;
        static final int DIVIDE = 3;
        static final int ADD = 4;
        static final int SUBTRACT = 5;
    }

    static class INPUT {
        static final int AVERAGE = 0;
        static final int INTENSITY = 1;
        static final int CHANNEL = 2;
        static final int VALUE = 3;
    }
    
    static class CHANNEL {
        static final int ALPHA = 0;
        static final int RED = 1;
        static final int GREEN = 2;
        static final int BLUE = 3;
    }

    static class Shader {
        final int mode, target, targetChannel;
        final int input, inputMode, inputChannel;
        final float inputValue;

        Shader(int mode, int target, int targetChannel, int input,
                int inputMode, int inputChannel, float inputValue) {
            this.mode = mode;
            this.target = target;
            this.targetChannel = targetChannel;
            this.input = input;
            this.inputMode = inputMode;
            this.inputChannel = inputChannel;
            this.inputValue = inputValue;
        }
    }
    
    static class ShaderUses {
        final boolean buffer, icon_intensity, buffer_intensity, output_intensity;
        
        ShaderUses(List<Shader> shaders){
            boolean buffer = false, icon_intensity = false, buffer_intensity = false, output_intensity = false;
            for(Shader s : shaders) {
                if (s.mode == MODE.NONE)
                    continue;
                if (s.input == IMAGE.BUFFER || s.target == IMAGE.BUFFER)
                    buffer = true;
                if (s.inputMode == INPUT.INTENSITY)
                    switch(s.input) {
                    case IMAGE.ICON:
                        icon_intensity = true;
                        break;
                    case IMAGE.BUFFER:
                        buffer_intensity = true;
                        break;
                    case IMAGE.OUTPUT:
                        output_intensity = true;
                        break;
                    }
            }
            this.buffer = buffer;
            this.icon_intensity = icon_intensity;
            this.buffer_intensity = buffer_intensity;
            this.output_intensity = output_intensity;
        }
    }
    
    static class CompiledIconShader {
        static final int MAXLENGTH = 5184; // 72*72
        
        final List<Shader> shaders;
        final ShaderUses uses;
        
        // array references held here so that they are only allocated once
        final int[] pixels;
        final float[][] icon, buffer, output;
        final float[] icon_intensity, buffer_intensity, output_intensity;
       
        CompiledIconShader(List<Shader> s) {
            shaders = s;
            uses = new ShaderUses(s);
            pixels = new int[MAXLENGTH];
            icon = new float[4][MAXLENGTH];
            output = new float[4][MAXLENGTH];
            
            if (uses.buffer)
                buffer = new float[4][MAXLENGTH];
            else buffer = null;
            
            if (uses.icon_intensity) 
                icon_intensity = new float[MAXLENGTH];
            else icon_intensity = null;
            if (uses.buffer_intensity) 
                buffer_intensity = new float[MAXLENGTH];
            else buffer_intensity = null;
            if (uses.output_intensity) 
                output_intensity = new float[MAXLENGTH];
            else output_intensity = null;
        }
    }

    static CompiledIconShader parseXml(XmlResourceParser xpp) {
        List<Shader> shaders = new LinkedList<Shader>();
        Shader s;
        String a0, a1, a2;
        try {
            int eventType = xpp.getEventType();
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                if (eventType == XmlResourceParser.START_TAG
                        && xpp.getName().compareTo("exec") == 0
                        && xpp.getAttributeCount() == 3) {

                    a0 = xpp.getAttributeValue(0);
                    a1 = xpp.getAttributeValue(1);
                    a2 = xpp.getAttributeValue(2);
                    s = createShader(a0, a1, a2);
                    if (s != null)
                        shaders.add(s);
                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            return null;
        }
        return new CompiledIconShader(shaders);
    }

    private static Shader createShader(String targetStr, String modeStr, String inputStr) {
        int mode = MODE.NONE;
        int target = IMAGE.OUTPUT;
        int targetChannel = CHANNEL.ALPHA;
        int input = IMAGE.ICON;
        int inputMode = INPUT.CHANNEL;
        int inputChannel = CHANNEL.ALPHA;
        float inputValue = 0;
        try {
            switch (modeStr.charAt(0)) {
            case 'W':
                mode = MODE.WRITE;
                break;
            case 'M':
                mode = MODE.MULTIPLY;
                break;
            case 'D':
                mode = MODE.DIVIDE;
                break;
            case 'A':
                mode = MODE.ADD;
                break;
            case 'S':
                mode = MODE.SUBTRACT;
                break;
            default:
                throw (new Exception());
            }

            switch (targetStr.charAt(0)) {
            case 'B':
                target = IMAGE.BUFFER;
                break;
            case 'O':
                target = IMAGE.OUTPUT;
                break;
            default:
                throw (new Exception());
            }
            switch (targetStr.charAt(1)) {
            case 'A':
                targetChannel = CHANNEL.ALPHA;
                break;
            case 'R':
                targetChannel = CHANNEL.RED;
                break;
            case 'G':
                targetChannel = CHANNEL.GREEN;
                break;
            case 'B':
                targetChannel = CHANNEL.BLUE;
                break;
            default:
                throw (new Exception());
            }

            boolean isValue = false;
            switch (inputStr.charAt(0)) {
            case 'I':
                input = IMAGE.ICON;
                break;
            case 'B':
                input = IMAGE.BUFFER;
                break;
            case 'O':
                input = IMAGE.OUTPUT;
                break;
            default:
                inputValue = Float.parseFloat(inputStr);
                isValue = true;
                inputMode = INPUT.VALUE;
                ;
            }
            if (!isValue)
                switch (inputStr.charAt(1)) {
                case 'A':
                    inputChannel = CHANNEL.ALPHA;
                    break;
                case 'R':
                    inputChannel = CHANNEL.RED;
                    break;
                case 'G':
                    inputChannel = CHANNEL.GREEN;
                    break;
                case 'B':
                    inputChannel = CHANNEL.BLUE;
                    break;
                case 'I':
                    inputMode = INPUT.INTENSITY;
                    break;
                case 'H':
                    inputMode = INPUT.AVERAGE;
                    break;
                default:
                    throw (new Exception());
                }
        } catch (Exception e) {
        }

        return new Shader(mode, target, targetChannel, input, inputMode,
                inputChannel, inputValue);
    }
    
    static Drawable processIcon(Drawable icon_d, CompiledIconShader compiledShader) {
        List<Shader> shaders = compiledShader.shaders;
        Bitmap icon_bitmap=null;
        // get bitmap
        if (icon_d instanceof BitmapDrawable) {
            BitmapDrawable icon_bd = (BitmapDrawable) icon_d;
            icon_bitmap = icon_bd.getBitmap();
        } else if(icon_d instanceof FastBitmapDrawable) {
            FastBitmapDrawable icon_bd = (FastBitmapDrawable) icon_d;
            icon_bitmap = icon_bd.getBitmap();
        } else
            return null;
        if (icon_bitmap == null)
            return null;
        
        int width = icon_bitmap.getWidth();
        int height = icon_bitmap.getHeight();
        int length = width * height;
        if (length > CompiledIconShader.MAXLENGTH)
            return null;
        
        int[] pixels = compiledShader.pixels;
        float[][] icon = compiledShader.icon;
        float[][] buffer = compiledShader.buffer;
        float[][] output = compiledShader.output;
        
        float icon_average = 0;
        float buffer_average = 0;
        float output_average = 0;
        boolean icon_average_valid = false;
        boolean buffer_average_valid = false;
        boolean output_average_valid = false;
        
        float[] icon_intensity = compiledShader.icon_intensity;
        float[] buffer_intensity = compiledShader.buffer_intensity;;
        float[] output_intensity = compiledShader.output_intensity;
        
        boolean icon_intensity_valid = false;
        boolean buffer_intensity_valid = false;
        boolean output_intensity_valid = false;
        
        // convert to float
        icon_bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < length; i++) {
            icon[CHANNEL.BLUE][i] = pixels[i] & 0x000000FF;
            icon[CHANNEL.GREEN][i] = (pixels[i] >> 8) & 0x000000FF;
            icon[CHANNEL.RED][i] = (pixels[i] >> 16) & 0x000000FF;
            icon[CHANNEL.ALPHA][i] = (pixels[i] >> 24) & 0x000000FF;
        }
        
        // temporary pointers/values
        float inputValue = 0;
        float[] inputArray = null;
        float[] targetArray = null;
        // process each shader
        for (Shader s : shaders) {

            if (s.mode == MODE.NONE)
                continue;

            // determine input
            if (s.inputMode == INPUT.AVERAGE) {
                switch (s.input) {
                case IMAGE.ICON:
                    if (!icon_average_valid) {
                        icon_average = getAverage(icon, length);
                        icon_average_valid = true;
                    }
                    inputValue = icon_average;
                    break;
                case IMAGE.BUFFER:
                    if (!buffer_average_valid) {
                        buffer_average = getAverage(buffer, length);
                        buffer_average_valid = true;
                    }
                    inputValue = buffer_average;
                    break;
                case IMAGE.OUTPUT:
                    if (!output_average_valid) {
                        output_average = getAverage(output, length);
                        output_average_valid = true;
                    }
                    inputValue = output_average;
                    break;
                }
            }
            if (s.inputMode == INPUT.INTENSITY) {
                switch (s.input) {
                case IMAGE.ICON:
                    if (!icon_intensity_valid) {
                        getIntensity(icon_intensity, icon, length);
                        icon_intensity_valid = true;
                    }
                    inputArray = icon_intensity;
                    break;
                case IMAGE.BUFFER:
                    if (!buffer_intensity_valid) {
                        getIntensity(buffer_intensity, buffer, length);
                        buffer_intensity_valid = true;
                    }
                    inputArray = buffer_intensity;
                    break;
                case IMAGE.OUTPUT:
                    if (!output_intensity_valid) {
                        getIntensity(output_intensity, output, length);
                        output_intensity_valid = true;
                    }
                    inputArray = output_intensity;
                    break;
                }
            }
            if (s.inputMode == INPUT.CHANNEL) {
                switch (s.input) {
                case IMAGE.ICON:
                    inputArray = icon[s.inputChannel];
                    break;
                case IMAGE.BUFFER:
                    inputArray = buffer[s.inputChannel];
                    break;
                case IMAGE.OUTPUT:
                    inputArray = output[s.inputChannel];
                    break;
                }
            }
            if (s.inputMode == INPUT.VALUE) {
                inputValue = s.inputValue;
            }

            // determine target
            if (s.target == IMAGE.BUFFER) {
                targetArray = buffer[s.targetChannel];
            }
            if (s.target == IMAGE.OUTPUT) {
                targetArray = output[s.targetChannel];
                //main.text += "target output\n";
            }
            
            // write to target
            switch (s.mode) {
            case MODE.WRITE:
                if (s.inputMode == INPUT.AVERAGE || s.inputMode == INPUT.VALUE) {
                    Arrays.fill(targetArray, inputValue);
                }
                if (s.inputMode == INPUT.INTENSITY
                        || s.inputMode == INPUT.CHANNEL) {
                    System.arraycopy(inputArray, 0, targetArray, 0, length);
                }
                break;
            case MODE.MULTIPLY:
                if (s.inputMode == INPUT.AVERAGE || s.inputMode == INPUT.VALUE) {
                    for (int i = 0; i < length; i++)
                        targetArray[i] *= inputValue;
                }
                if (s.inputMode == INPUT.INTENSITY
                        || s.inputMode == INPUT.CHANNEL) {
                    for (int i = 0; i < length; i++)
                        targetArray[i] *= inputArray[i];
                }
                break;
            case MODE.DIVIDE:
                if (s.inputMode == INPUT.AVERAGE || s.inputMode == INPUT.VALUE) {
                    // multiply by 1/value
                    inputValue = 1 / inputValue;
                    for (int i = 0; i < length; i++)
                        targetArray[i] *= inputValue;
                }
                if (s.inputMode == INPUT.INTENSITY
                        || s.inputMode == INPUT.CHANNEL) {
                    for (int i = 0; i < length; i++)
                        targetArray[i] /= inputArray[i];
                }
                break;
            case MODE.ADD:
                if (s.inputMode == INPUT.AVERAGE || s.inputMode == INPUT.VALUE) {
                    for (int i = 0; i < length; i++)
                        targetArray[i] += inputValue;
                }
                if (s.inputMode == INPUT.INTENSITY
                        || s.inputMode == INPUT.CHANNEL) {
                    for (int i = 0; i < length; i++)
                        targetArray[i] += inputArray[i];
                }
                break;
            case MODE.SUBTRACT:
                if (s.inputMode == INPUT.AVERAGE || s.inputMode == INPUT.VALUE) {
                    for (int i = 0; i < length; i++)
                        targetArray[i] -= inputValue;
                }
                if (s.inputMode == INPUT.INTENSITY
                        || s.inputMode == INPUT.CHANNEL) {
                    for (int i = 0; i < length; i++)
                        targetArray[i] -= inputArray[i];
                }
                break;
            }
            
            // invalidate average/intensity
            switch (s.target) {
            case IMAGE.BUFFER:
                buffer_average_valid = false;;
                buffer_intensity_valid = false;
                break;
            case IMAGE.OUTPUT:
                output_average_valid = false;
                output_intensity_valid = false;
                break;
            }
        }
        // finished processing
        
        // convert back to 32bit color
        int a, r, g, b;
        for (int i = 0; i < length; i++) {
            a = (int) output[CHANNEL.ALPHA][i];
            r = (int) output[CHANNEL.RED][i];
            g = (int) output[CHANNEL.GREEN][i];
            b = (int) output[CHANNEL.BLUE][i];

            a = a > 255 ? 255 : a < 0 ? 0 : a;
            r = r > 255 ? 255 : r < 0 ? 0 : r;
            g = g > 255 ? 255 : g < 0 ? 0 : g;
            b = b > 255 ? 255 : b < 0 ? 0 : b;

            a <<= 8;
            a |= r;
            a <<= 8;
            a |= g;
            a <<= 8;
            a |= b;
            pixels[i] = a;
        }
        
        // build drawable
        Bitmap.Config c = (icon_bitmap.getConfig()==null) ?
                Bitmap.Config.ARGB_8888 : icon_bitmap.getConfig();
        Bitmap output_bitmap = Bitmap.createBitmap(pixels, width, height, c);
        output_bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        BitmapDrawable output_bd = new BitmapDrawable(output_bitmap);
        return output_bd;
    }
    
    private static float getAverage(float[][] array, int length) {
        double average = 0;
        double total = 0;
        for (int i = 0; i < length; i++) {
            average += array[0][i] * (array[1][i] + array[2][i] + array[3][i])
                    / 3;
            total += array[0][i];
        }
        average /= total;
        return (float) average;
    }

    private static void getIntensity(float[] intensity, float[][] array, int length) {
        for (int i = 0; i < length; i++)
            intensity[i] = (array[1][i] + array[2][i] + array[3][i]) / 3;
    }
}