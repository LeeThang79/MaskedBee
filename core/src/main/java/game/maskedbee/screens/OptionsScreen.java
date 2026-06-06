package game.maskedbee.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import game.maskedbee.main.CORE;
import game.maskedbee.main.AudioManager;

public class OptionsScreen implements Screen {
    private final CORE game;
    private Stage stage;

    private BitmapFont titleFont;
    private BitmapFont bodyFont;
    private BitmapFont hintFont;

    // Biến lưu âm lượng toàn cục 
    private static float volumeSetting = AudioManager.getInstance().getVolume();

    public OptionsScreen(CORE game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport(), game.batch);
        Gdx.input.setInputProcessor(stage); // Cho phép chuột tương tác với màn hình này

        // 1. TẠO 3 CỠ FONT CHỮ TỪ MASKEDBEE.FNT
        titleFont = new BitmapFont(Gdx.files.internal("MaskedBee.fnt"));
        titleFont.getData().setScale(1.8f);

        bodyFont = new BitmapFont(Gdx.files.internal("MaskedBee.fnt"));
        bodyFont.getData().setScale(1.0f);

        hintFont = new BitmapFont(Gdx.files.internal("MaskedBee.fnt"));
        hintFont.getData().setScale(0.8f);

        // Tạo Style cho Label
        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, Color.GOLD);
        Label.LabelStyle bodyStyle = new Label.LabelStyle(bodyFont, Color.WHITE);
        Label.LabelStyle hintStyle = new Label.LabelStyle(hintFont, Color.LIGHT_GRAY);

        // 2. TẠO BẢNG CHÍNH (TABLE) ĐỂ CĂN LỀ TỰ ĐỘNG
        Table mainTable = new Table();
        mainTable.setFillParent(true); // Trải rộng toàn màn hình
        // mainTable.setDebug(true); // (Mẹo: Bỏ comment dòng này để thấy các ô viền đỏ căn lề)

        // --- PHẦN 1: TIÊU ĐỀ ---
        mainTable.add(new Label("--- OPTIONS ---", titleStyle)).colspan(2).padBottom(40).row();

        // --- PHẦN 2: ÂM LƯỢNG (DÙNG SLIDER KÉO THẢ) ---
        Label volumeLabel = new Label("MUSIC VOLUME", bodyStyle);
        mainTable.add(volumeLabel).align(Align.right).padRight(20).padBottom(30);

        // Thiết lập giao diện cho thanh trượt Slider bằng mã màu vuông vức (Pixel art style)
        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        // Nền thanh trượt (Màu xám tối)
        sliderStyle.background = createColorDrawable(300, 10, new Color(0.2f, 0.2f, 0.2f, 1f));
        // Phần đã kéo qua (Màu vàng)
        sliderStyle.knobBefore = createColorDrawable(300, 10, Color.GOLD);
        // Cục nắm để kéo (Màu trắng vuông)
        sliderStyle.knob = createColorDrawable(16, 24, Color.WHITE);

        // Khởi tạo Slider (từ 0.0 đến 1.0, mỗi bước nhỏ 0.05)
        final Slider volumeSlider = new Slider(0f, 1f, 0.05f, false, sliderStyle);
        volumeSlider.setValue(volumeSetting); // Set giá trị hiện tại

        // Gắn sự kiện khi lấy chuột kéo thanh trượt
        volumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                volumeSetting = volumeSlider.getValue();
                AudioManager.getInstance().setVolume(volumeSetting); // Phát nhạc to/nhỏ ngay lập tức
            }
        });

        // Tạo phần chữ % bên cạnh thanh trượt
        final Label percentageLabel = new Label(Math.round(volumeSetting * 100) + "%", bodyStyle);
        percentageLabel.setColor(Color.YELLOW);

        // Nhóm thanh trượt và chữ % vào 1 cái table nhỏ cho ngay ngắn
        Table sliderTable = new Table();
        sliderTable.add(volumeSlider).width(300).padRight(15);
        sliderTable.add(percentageLabel).width(50).align(Align.left);

        mainTable.add(sliderTable).align(Align.left).padBottom(30).row();

        // Gắn sự kiện cập nhật số % liên tục khi kéo
        volumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                percentageLabel.setText(Math.round(volumeSlider.getValue() * 100) + "%");
            }
        });

        // --- PHẦN 3: HƯỚNG DẪN CÁCH CHƠI ---
        mainTable.add(new Label("--- CONTROLS ---", titleStyle)).colspan(2).padBottom(20).padTop(20).row();

        // Danh sách các nút (Căn trái toàn bộ cho giống cột tài liệu)
        float rowPadding = 10f;
        addControlRow(mainTable, "W, A, S, D / Arrows", "Move Character", hintStyle, rowPadding);
        addControlRow(mainTable, "G", "Examine Points / Push Cocoons", hintStyle, rowPadding);
        addControlRow(mainTable, "E", "Pull Levers / Interact", hintStyle, rowPadding);
        addControlRow(mainTable, "F", "Unlock Doors / Hide", hintStyle, rowPadding);
        addControlRow(mainTable, "SPACE / ENTER", "Pass Room / Select", hintStyle, rowPadding);
        addControlRow(mainTable, "R", "Reset Room Puzzle", hintStyle, rowPadding);

        // --- PHẦN 4: NÚT QUAY LẠI ---
        final Label returnLabel = new Label("RETURN", titleStyle);
        returnLabel.setColor(Color.LIGHT_GRAY);

        // Hiệu ứng sáng lên khi rê chuột vào chữ RETURN
        returnLabel.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                returnLabel.setColor(Color.WHITE); // Sáng lên
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                returnLabel.setColor(Color.LIGHT_GRAY); // Tối đi
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Thoát ra Main Menu khi click
                game.setScreen(new FirstScreen(game));
            }
        });

        mainTable.add(returnLabel).colspan(2).padTop(50).row();

        // Gắn bảng vào sân khấu
        stage.addActor(mainTable);
    }

    // Hàm tiện ích để thêm từng dòng Control vào bảng cho thẳng hàng
    private void addControlRow(Table table, String keyString, String actionString, Label.LabelStyle style, float padBottom) {
        Label keyLabel = new Label(keyString, style);
        keyLabel.setColor(Color.CYAN); // Tô màu chữ phím bấm cho nổi

        Label actionLabel = new Label(actionString, style);

        // Cột trái: Tên phím (Căn lề phải) | Cột phải: Hành động (Căn lề trái)
        table.add(keyLabel).align(Align.right).padRight(20).padBottom(padBottom);
        table.add(actionLabel).align(Align.left).padBottom(padBottom).row();
    }

    // Hàm tạo Texture màu trơn (dùng làm thanh kéo Slider vuông vức)
    private TextureRegionDrawable createColorDrawable(int width, int height, Color color) {
        // Tạo một bức ảnh vô hình trong RAM, tô màu, rồi quấn nó thành một cục gạch cho Slider
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(width, height, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose(); // Xóa rác RAM ngay
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    @Override
    public void render(float delta) {
        // Vẽ màn nền tối xám cho sang trọng (thay vì đen sì)
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Chạy hiệu ứng rê chuột và vẽ UI lên màn hình
        stage.act(delta);
        stage.draw();

        // Hỗ trợ thoát bằng phím ESCAPE cho tiện
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new FirstScreen(game));
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        titleFont.dispose();
        bodyFont.dispose();
        hintFont.dispose();
    }
}
