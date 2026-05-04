package game.maskedbee.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.OrthographicCamera;
import game.maskedbee.main.CORE;

public class FirstScreen implements Screen {
    private final CORE game;
    private Stage stage;
    private Viewport viewport;
    private OrthographicCamera camera;
    private Texture background;
    private Texture masked;
    private Texture newGame;
    private Texture options;
    private Texture quit;
    private Texture pointer;


    public FirstScreen(CORE game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        // Giả sử độ phân giải màn hình mong muốn là độ phân giải thực của ảnh background
        // Điều chỉnh giá trị này nếu cần
        camera.setToOrtho(false, 640, 480);
        viewport = new ScreenViewport(camera);
        stage = new Stage(viewport, game.batch);
        Gdx.input.setInputProcessor(stage); // Cho phép sân khấu nhận tương tác chuột/phím

        masked = new Texture("menu/masked.png");
        newGame = new Texture("menu/newgame.png");
        options = new Texture("menu/options.png");
        quit = new Texture("menu/quit.png");
        pointer = new Texture("menu/pointer.png");
        background = new Texture("menu/menu_background.png");

        // 3. Tạo Table để sắp xếp và căn chỉnh giao diện
        Table mainTable = new Table();
        mainTable.setFillParent(true); // Table bao phủ toàn bộ màn hình
        mainTable.left(); // Căn chỉnh menu ở giữa chiều cao và bên trái
        mainTable.padLeft(120); // Thêm lề trái lớn một chút để menu không dính mép

        // 4. Thêm nút tiêu đề (MASKED)
        Image maskedImage = new Image(masked);
        maskedImage.setScale(2.5f);
        // Cách các nút dưới 60px, xuống dòng
        mainTable.add(maskedImage).left().padLeft(35).padBottom(50).row();

        mainTable.add(createMenuOption(newGame, "NEW GAME", new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new PlayScreen(game));
            }
        })).left().padBottom(20).row();

        mainTable.add(createMenuOption(options, "OPTIONS", new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Code để mở màn hình Options (chưa làm)
                Gdx.app.log("Menu", "Clicked Options - Chưa có tính năng");
            }
        })).left().padBottom(20).row();

        mainTable.add(createMenuOption(quit, "QUIT", new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit(); // Thoát game
            }
        })).left().row();

        // 6. Đưa Table chính lên sân khấu
        stage.addActor(mainTable);
    }

    private Table createMenuOption(Texture text, String name, ClickListener clickListener) {
        Table optionTable = new Table();
        final Image pointerImage = new Image(pointer);
        pointerImage.setVisible(false); // Mũi tên ẩn mặc định

        final Image textImage = new Image(text);
        textImage.setName(name); // Thêm tên để dễ debug
        //Set lại scale cho pointer
        float scaledWidth = text.getWidth() * 1.9f;
        float scaledHeight = text.getHeight() * 1.9f;

        // Thêm Mũi tên và Text vào Table nhỏ, cách nhau 15px
        optionTable.add(pointerImage).size(20, 20).padRight(15).center(); // Kích thước pointer pixel art
        optionTable.add(textImage).size(scaledWidth,scaledHeight).left().center(); // Text căn trái

        // Thêm tương tác di chuột (Hover) cho textImage
        textImage.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                pointerImage.setVisible(true); // Hiện mũi tên khi di chuột vào text
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                pointerImage.setVisible(false); // Ẩn mũi tên khi di chuột ra text
            }
        });

        // Thêm click listener cho textImage
        textImage.addListener(clickListener);

        return optionTable;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        game.batch.begin();
        game.batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        game.batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
        camera.update();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        if (background != null) background.dispose();
        if (masked != null) masked.dispose();
        if (newGame != null) newGame.dispose();
        if (options != null) options.dispose();
        if (quit != null) quit.dispose();
        if (pointer != null) pointer.dispose();
    }
}

