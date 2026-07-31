    ImageButton button;
    
    void setup(){
      size(300, 300);
      // button dimensions
      int w = 99;
      int h = 25;
      
      button = new ImageButton(100, 150, w, h, 
                   new PImage[]{
                     // use loadImage with your own images instead of getImage :)
                     getImage(w, h, color(192, 0, 32 * 2)), // off
                     getImage(w, h, color(0, 0, 32 * 3)), // 10
                     getImage(w, h, color(0, 0, 32 * 4)), // 20
                     getImage(w, h, color(0, 0, 32 * 5)), // 30
                     getImage(w, h, color(0, 0, 32 * 6)), // 40
                     getImage(w, h, color(0, 0, 32 * 7)), // 50
                     getImage(w, h, color(0, 0, 32 * 8)), // 60
                   });
    }
    
    void draw(){
      background(0);
      button.draw();
    }
    
    void mousePressed(){
      button.mousePressed(mouseX,mouseY);
    }
    
    // test images to represent loaded state images
    PImage getImage(int w, int h, int c){
      PImage img = createImage(w, h, RGB);
      java.util.Arrays.fill(img.pixels, c);
      img.updatePixels();
      return img;
    }
    
    
    class ImageButton{
      int min = 0;
      // images for each state
      PImage[] stateImages;
      // which image to display
      int stateIndex;
      // position
      int x, y;
      // dimensions: width , height
      int w, h;
      // text to display
      String label = "????";
      
      ImageButton(int x, int y, int w, int h, PImage[] stateImages){
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.stateImages = stateImages;
      }
      
      void mousePressed(int mx, int my){
        // check the cursor is within the button bounds
        boolean isOver = ((mx >= x && mx <= x + w) &&  // check horizontal
                          (my >= y && my <= y + h) );  // check vertical
                  
        if(isOver){
          
          min += 10;
          stateIndex++;
          
          if (min>60) {
            min = 0; 
            stateIndex = 0;
            label = "????";
          } else  {
            label = min + " ???";
          }
          
        }
      }
      
      void draw(){
        fill(255);
        rect(x,y,w,h);
        fill(127);
        // if the images and index are valid
        if(stateImages != null && stateIndex < stateImages.length){
          image(stateImages[stateIndex], x, y, w, h);
        }
        // display text
        text(label, x, y + h + 12);
      }
      
    }