/*
    Copyright (c) 2007, Interactive Pulp, LLC
    All rights reserved.
    
    Redistribution and use in source and binary forms, with or without 
    modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright 
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright 
          notice, this list of conditions and the following disclaimer in the 
          documentation and/or other materials provided with the distribution.
        * Neither the name of Interactive Pulp, LLC nor the names of its 
          contributors may be used to endorse or promote products derived from 
          this software without specific prior written permission.
    
    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
    POSSIBILITY OF SUCH DAMAGE.
*/

package pulpcore.image;

import pulpcore.CoreSystem;
import pulpcore.math.CoreMath;

/**
    An AnimatedImage is a CoreImage that contains multiple frames of animation.
*/
public class AnimatedImage extends CoreImage {
    
    private CoreImage[] frames;
    
    private int[] frameSequence;
    private int[] frameDuration;
    private boolean loop;
    
    // fields used during animation
    private boolean playing;
    private int animTime;
    private int currentFrame;
    
    
    /**
        Creates a copy of the specified AnimatedImage. The internal raster data array is shared.
    */
    public AnimatedImage(AnimatedImage image) {
        super(image);
        
        frames = new CoreImage[image.frames.length];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = new CoreImage(image.frames[i]);
        }
        setSequence(image.frameSequence, image.frameDuration, image.loop);
        
        setFrame(0);
        playing = true;
    }
    
    
    /**
        Creates an AnimatedImage by spliting a image into frames on a grid.
    */
    public AnimatedImage(CoreImage image, int numFramesAcross, int numFramesDown) { 
     
        super.width = image.getWidth() / numFramesAcross;
        super.height = image.getHeight() / numFramesDown;
        super.isOpaque = image.isOpaque;
        
        setHotspot(image.getHotspotX(), image.getHotspotY()); 
            
        frames = image.split(numFramesAcross, numFramesDown);
        
        setFrame(0);
        playing = true;
    }
    
    
    public boolean isLooping() {
        return loop;
    }
    

    public int getNumFrames() {
        if (frameSequence == null) {
            return frames.length;
        }
        else {
            return frameSequence.length;
        }
    }
    
    
    public int getDuration() {
        if (frameDuration == null) {
            return 0;
        }
        else {
            int duration = 0;
            for (int i = 0; i < frameDuration.length; i++) {
                duration += frameDuration[i];
            }
            return duration;
        }
    }
    
    
    public void setFrameDuration(int duration, boolean loop) {
        if (frameDuration == null) {
            frameDuration = new int[frames.length];
        }
        
        this.loop = loop;
        
        for (int i = 0; i < frameDuration.length; i++) {
            frameDuration[i] = duration;
        }
        
        setFrame(0);
    }
    
    
    public void setSequence(int[] frameSequence, int[] frameDuration, boolean loop) {
        
        if (frameSequence == null) {
            this.frameSequence = null;
        }
        else {
            this.frameSequence = new int[frameSequence.length];
            System.arraycopy(frameSequence, 0, this.frameSequence, 0, frameSequence.length);
        }
        
        if (frameDuration == null) {
            this.frameDuration = null;
        }
        else {
            this.frameDuration = new int[frameDuration.length];
            System.arraycopy(frameDuration, 0, this.frameDuration, 0, frameDuration.length);
        }
            
        this.loop = loop;
        setFrame(0);
    }
    
    
    public void update(int elapsedTime) {
        super.update(elapsedTime);
        
        if (!playing || frameDuration == null) {
            return;
        }

        animTime += elapsedTime;
        
        while (animTime >= frameDuration[currentFrame]) {
            animTime -= frameDuration[currentFrame];
            if (currentFrame == frameDuration.length - 1) {
                if (loop) {
                    setFrame(0);
                }
                else {
                    playing = false;
                    break;
                }
            }
            else {
                setFrame(currentFrame + 1);
            }
        }
    }
    
    
    public void setFrame(int frame) {
        currentFrame = frame;
        animTime = 0;
        if (frameSequence != null) {
            super.data = frames[frameSequence[currentFrame]].getData();
        }
        else {
            super.data = frames[currentFrame].getData();
        }
    }
    
    
    public int getFrame() {
        if (frameSequence != null) {
            return frameSequence[currentFrame];
        }
        else {
            return currentFrame;
        }
    }
    
    
    public boolean isPlaying() {
        return playing;
    }
    
    
    public void start() {
        playing = true;
    }
    
    
    public void pause() {
        playing = false;
    }
    
    
    public void stop() {
        playing = false;
        setFrame(0);
    }
    
    
    //
    // Image Manipulation
    //

    public CoreImage crop(int x, int y, int w, int h) {
        AnimatedImage newImage = new AnimatedImage(this);
        newImage.width = w;
        newImage.height = h;
        
        for (int i = 0; i < frames.length; i++) {
            newImage.frames[i] = frames[i].crop(x, y, w, h);
        }
        
        newImage.setFrame(0);
        
        return newImage;
    }
    
    
    public CoreImage rotate(int angle, boolean sizeAsNeeded) {
        int newWidth = width;
        int newHeight = height;
        
        int cos = CoreMath.cos(angle);
        int sin = CoreMath.sin(angle);
        
        if (sizeAsNeeded) {
            newWidth = CoreMath.toIntCeil(Math.abs(width * cos) + Math.abs(height * sin));
            newHeight = CoreMath.toIntCeil(Math.abs(width * sin) + Math.abs(height * cos));
        }
        
        AnimatedImage newImage = new AnimatedImage(this);
        newImage.width = newWidth;
        newImage.height = newHeight;
        
        int x = getHotspotX() - width/2;
        int y = getHotspotY() - height/2;
        newImage.setHotspot(
            CoreMath.toIntRound(x * cos - y * sin) + newWidth / 2,
            CoreMath.toIntRound(x * sin + y * cos) + newHeight / 2);
            
            
        for (int i = 0; i < frames.length; i++) {
            newImage.frames[i] = frames[i].rotate(angle, sizeAsNeeded);
        }
        
        newImage.setFrame(0);
        
        return newImage;
    }
    
    
    public CoreImage halfSize() {
        AnimatedImage newImage = new AnimatedImage(this);
        newImage.width = width/2;
        newImage.height = height/2;
        newImage.setHotspot(
            getHotspotX() / 2,
            getHotspotY() / 2);
        
        for (int i = 0; i < frames.length; i++) {
            newImage.frames[i] = frames[i].halfSize();
        }
        
        newImage.setFrame(0);
        
        return newImage;
    }
    
    
    public CoreImage scale(int scaledFrameWidth, int scaledFrameHeight) {
        
        AnimatedImage newImage = new AnimatedImage(this);
        newImage.width = scaledFrameWidth;
        newImage.height = scaledFrameHeight;
        newImage.setHotspot(
            getHotspotX() * scaledFrameWidth / width,
            getHotspotY() * scaledFrameHeight / height);
        
        for (int i = 0; i < frames.length; i++) {
            newImage.frames[i] = frames[i].scale(scaledFrameWidth, scaledFrameHeight);
        }
        
        newImage.setFrame(0);
        
        return newImage;
    }
    
    
    public CoreImage mirror() {
        
        AnimatedImage newImage = new AnimatedImage(this);
        newImage.setHotspot(width - 1 - getHotspotX(), getHotspotY());
        
        for (int i = 0; i < frames.length; i++) {
            newImage.frames[i] = frames[i].mirror();
        }
        
        newImage.setFrame(0);
        
        return newImage;
    }
    
    
    public CoreImage flip() {
        AnimatedImage newImage = new AnimatedImage(this);
        newImage.setHotspot(getHotspotX(), height - 1 - getHotspotY());
        
        for (int i = 0; i < frames.length; i++) {
            newImage.frames[i] = frames[i].flip();
        }
        
        newImage.setFrame(0);
        
        return newImage;
    }
    
    
    public CoreImage rotateLeft() {
        AnimatedImage newImage = new AnimatedImage(this);
        newImage.width = height;
        newImage.height = width;
        newImage.setHotspot(getHotspotY(), (width - 1) - getHotspotX());
        
        for (int i = 0; i < frames.length; i++) {
            newImage.frames[i] = frames[i].rotateLeft();
        }
        
        newImage.setFrame(0);
        
        return newImage;
    }
    
    
    public CoreImage rotateRight() {
        AnimatedImage newImage = new AnimatedImage(this);
        newImage.width = height;
        newImage.height = width;
        newImage.setHotspot((height - 1) - getHotspotX(), getHotspotY());
        
        for (int i = 0; i < frames.length; i++) {
            newImage.frames[i] = frames[i].rotateRight();
        }
        
        newImage.setFrame(0);
        
        return newImage;
    }
    
    public CoreImage rotate180() {
        AnimatedImage newImage = new AnimatedImage(this);
        newImage.setHotspot((width - 1) - getHotspotX(), (height - 1) - getHotspotY());
        
        for (int i = 0; i < frames.length; i++) {
            newImage.frames[i] = frames[i].rotate180();
        }
        
        newImage.setFrame(0);
        
        return newImage;
    }
    
    
    public CoreImage tint(int rgbColor) {
        AnimatedImage newImage = new AnimatedImage(this);
        
        for (int i = 0; i < frames.length; i++) {
            newImage.frames[i] = frames[i].tint(rgbColor);
        }
        
        newImage.setFrame(0);
        
        return newImage;
    }
    
    
    public CoreImage background(int argbColor, boolean hasAlpha) {

        AnimatedImage newImage = new AnimatedImage(this);
        
        for (int i = 0; i < frames.length; i++) {
            newImage.frames[i] = frames[i].background(argbColor, hasAlpha);
        }
        
        newImage.setFrame(0);
        
        return newImage;
    }
    
    
    public CoreImage fade(int alpha) {
        AnimatedImage newImage = new AnimatedImage(this);
        
        for (int i = 0; i < frames.length; i++) {
            newImage.frames[i] = frames[i].fade(alpha);
        }
        
        newImage.setFrame(0);
        
        return newImage;
    }
}
