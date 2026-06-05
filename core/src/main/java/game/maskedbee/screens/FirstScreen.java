package game.maskedbee.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.OrthographicCamera;
import game.maskedbee.main.CORE;
import game.maskedbee.main.AudioManager;

public class FirstScreen implements Screen {
    private final CORE game;
    private Stage stage;
    private Viewport viewport;
    private OrthographicCamera camera;
    private Texture background;
    private Texture pointer;

    private BitmapFont fontMenu;
    private BitmapFont fontTitle;

    private Label.LabelStyle labelStyle;
    private Label.LabelStyle titleStyle;

    public FirstScreen(CORE game) {
        this.game = game;
    }

    @Override
    public void show() {

        camera = new OrthographicCamera();

        // dung FitViewpoint nen doi thanh cai nay
        float virtualWidth = 960;
        float virtualHeight = 540;

        camera.setToOrtho(false, virtualWidth, virtualHeight);
        viewport = new FitViewport(virtualWidth, virtualHeight, camera);
        //--

        stage = new Stage(viewport, game.batch);
        Gdx.input.setInputProcessor(stage); // Cho phép sân khấu nhận tương tác chuột/phím

        // =========================================================================
        // KHỞI TẠO VÀ GENERATE FONT TỪ FILE .TTF
        // =========================================================================
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("MaskedBee.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();


        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "ẮẦÙỌÁẾỤĐƠáàảãạăắằẳẵặâấầẩẫậéèẻẽẹêếềểễệíìĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵđ";
        parameter.size = 24;
        parameter.color = Color.WHITE;
        // Nếu game là pixel art, bật dòng này để font chữ vuông vức không bị nhòe:
        parameter.minFilter = Texture.TextureFilter.Nearest;
        parameter.magFilter = Texture.TextureFilter.Nearest;
        fontMenu = generator.generateFont(parameter);

        // Cấu hình font cho phần Tiêu đề
        parameter.size = 64; // Kích cỡ chữ cho tiêu đề lớn hơn
        parameter.color = Color.WHITE;
        fontTitle = generator.generateFont(parameter);

        generator.dispose();

        labelStyle = new Label.LabelStyle();
        labelStyle.font = fontMenu;

        titleStyle = new Label.LabelStyle();
        titleStyle.font = fontTitle;

        pointer = new Texture("menu/pointer.png");
        background = new Texture("menu/menu_background.png");


        // 3. Tạo Table để sắp xếp và căn chỉnh giao diện
        Table mainTable = new Table();
        mainTable.setFillParent(true); // Table bao phủ toàn bộ màn hình
        mainTable.left(); // Căn chỉnh menu ở giữa chiều cao và bên trái
        mainTable.padLeft(60); // Thêm lề trái lớn một chút để menu không dính mép

        Label maskedLabel = new Label("MASKED", titleStyle);
        mainTable.add(maskedLabel).left().padLeft(35).padBottom(50).row();

        mainTable.add(createMenuOption("BẮT ĐẦU CHƠI", new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new PlayScreen(game));
            }
        })).left().padBottom(20).row();

        mainTable.add(createMenuOption("TÙY CHỌN", new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Code để mở màn hình Options (chưa làm)
                Gdx.app.log("Menu", "Clicked Options - Chưa có tính năng");
            }
        })).left().padBottom(20).row();

        mainTable.add(createMenuOption("THOÁT", new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit(); // Thoát game
            }
        })).left().row();

        // 6. Đưa Table chính lên sân khấu
        stage.addActor(mainTable);

        AudioManager.getInstance().playBackgroundMusic("audio/Memories.ogg", 0.4f);
    }

    private Table createMenuOption(String menuText, ClickListener clickListener) {
        Table optionTable = new Table();
        final Image pointerImage = new Image(pointer);
        pointerImage.setVisible(false); // Mũi tên ẩn mặc định

        // Thay Image cũ bằng Label chữ trực tiếp
        final Label textLabel = new Label(menuText, labelStyle);

        // Thêm Mũi tên và Text vào Table nhỏ, cách nhau 15px
        optionTable.add(pointerImage).size(20, 20).padRight(15).center(); // Kích thước pointer pixel art
        optionTable.add(textLabel).left().center(); // Text căn trái

        // Thêm tương tác di chuột (Hover) cho textImage
        textLabel.addListener(new ClickListener() {
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
        textLabel.addListener(clickListener);

        return optionTable;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        // Đồng bộ SpriteBatch với ma trận hiển thị của Camera/Viewport mới
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        // Vẽ background theo kích cỡ ảo gốc (640x480)
        game.batch.draw(background, 0, 0, camera.viewportWidth, camera.viewportHeight);
        game.batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height,true);
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
        if (pointer != null) pointer.dispose();
        if (fontMenu != null) fontMenu.dispose();
        if (fontTitle != null) fontTitle.dispose();
    }
}
