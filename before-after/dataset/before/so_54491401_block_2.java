	import javafx.animation.KeyFrame;
	import javafx.animation.Timeline;
	import javafx.application.Application;
	import javafx.geometry.Pos;
	import javafx.scene.Scene;
	import javafx.scene.control.Button;
	import javafx.scene.control.Label;
	import javafx.scene.layout.BorderPane;
	import javafx.scene.layout.HBox;
	import javafx.scene.layout.Priority;
	import javafx.scene.layout.VBox;
	import javafx.stage.Stage;
	import javafx.util.Duration;
	
	public class FxTest extends Application {
	
		private static final int X_TRANSLATE = 230;
		private VBox content;
		private Timeline openMenu, closeMenu;
	
		@Override
		public void start(Stage primaryStage) throws Exception{
	
			Label label = new Label("->");
			label.setMinWidth(20);
			label.setPrefWidth(20);
			label.setStyle("-fx-text-fill: orange; -fx-background-color: black");
	
			content = new VBox();
			content.setPrefWidth(0);
			content.setStyle("-fx-background-color: purple");
	
			HBox menu = new HBox(content,label);
			menu.setAlignment(Pos.CENTER);
			menu.setMaxWidth(250);
			menu.prefWidthProperty().bind(content.prefWidthProperty().add(label.prefWidthProperty()));
			menu.setStyle("-fx-background-color: yellow");
	
			BorderPane mainContent = new BorderPane(new Button("OKEY"),new Button("OKEY"),new Button("OKEY"),new Button("OKEY"),new Button("OKEY"));
			mainContent.setStyle("-fx-background-color: cyan");
	
			openMenu = new Timeline(
					new KeyFrame(Duration.millis(1), event -> setMenuSize(openMenu,1))
			);
			openMenu.setCycleCount(Timeline.INDEFINITE);
			
			closeMenu = new Timeline(
					new KeyFrame(Duration.millis(1), event -> setMenuSize(closeMenu, -1))
			);
			closeMenu.setCycleCount(Timeline.INDEFINITE);
	
			menu.setOnMouseEntered(evt -> openMenu.play());
			menu.setOnMouseExited(evt -> closeMenu.play());
	
			HBox root = new HBox(menu, mainContent);
			HBox.setHgrow(mainContent, Priority.ALWAYS);
	
			Scene scene = new Scene(root, 800, 600);
			primaryStage.setScene(scene);
			primaryStage.show();
		}
	
		private void setMenuSize(Timeline timeline, int i) {
	
			double width = content.getPrefWidth();
	
			if(width > X_TRANSLATE){
				timeline.stop();
				content.setPrefWidth(X_TRANSLATE);
	
			} else if (width < 0){
				timeline.stop();
				content.setPrefWidth(0);
			} else {
				content.setPrefWidth(width +i);
			}
		}
	
		public static void main(String[] args) {
			launch(null);
		}
	}