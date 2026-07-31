    ImageButton button;
    
    void setup(){
      size(300, 300);
      // button dimensions
      int w = 75;
      int h = 25;
      // test with generated images
      button = new ImageButton(112, 137, w, h, 
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
                   
      // loading images will be something similar to:
      //button = new ImageButton(112, 137, w, h, 
      //             new PImage[]{
      //               loadImage("0.png"), // off
      //               loadImage("1.png"), // 10
      //               loadImage("2.png"), // 20
      //               loadImage("3.png"), // 30
      //               loadImage("4.png"), // 40
      //               loadImage("5.png"), // 50
      //               loadImage("6.png"), // 60
      //             });
    }
    
    void draw(){
      background(0);
      button.draw();
    }
    
    void mousePressed(){
      button.mousePressed(mouseX,mouseY);
      println(button.min);
    }
    
    // test images to represent loaded state images
    PImage getImage(int w, int h, int c){
      PImage img = createImage(w, h, RGB);
      java.util.Arrays.fill(img.pixels, c);
      img.updatePixels();
      return img;
    }
    
    // make a custom image button class
    class ImageButton{
      // minutes is the data it stores
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
        // if the images and index are valid
        if(stateImages != null && stateIndex < stateImages.length){
          image(stateImages[stateIndex], x, y, w, h);
        }else{
          println("error displaying button state image");
          println("stateImages: ");
          printArray(stateImages);
          println("stateIndex: " + stateIndex);
        }
        // display text
        text(label, x + 5, y + h - 8);
      }
      
    }