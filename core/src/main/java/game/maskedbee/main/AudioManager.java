package game.maskedbee.main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;


public class AudioManager {
    private static AudioManager instance;

    private Music backgroundMusic;
    private String currentMusicPath = "";

    // Các biến phục vụ tính năng Fade (Nhỏ dần / To dần)
    private float targetVolume = 0.5f;     // Âm lượng đích mong muốn
    private float currentVolume = 0.5f;    // Âm lượng hiện tại đang phát
    private float fadeSpeed = 1.5f;        // Tốc độ fade (càng cao càng nhanh, 2.0f mất khoảng 0.5 giây)

    private boolean isFadingOut = false;   // Cờ đánh dấu đang nhỏ dần để tắt
    private String nextMusicPath = "";     // Lưu bài nhạc tiếp theo sẽ phát sau khi bài cũ tắt hẳn

    private AudioManager() {}

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    /**
     * Hàm phát nhạc nền có hỗ trợ Fade Out bài cũ (nếu có)
     */
    public void playBackgroundMusic(String filePath, float volume) {
        this.targetVolume = volume;

        // Nếu bài nhạc này đang phát rồi thì chỉ cập nhật lại âm lượng đích
        if (currentMusicPath.equals(filePath) && backgroundMusic != null && backgroundMusic.isPlaying()) {
            return;
        }

        // Nếu đang có một bài nhạc khác phát, kích hoạt hiệu ứng Fade Out nhỏ dần trước
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            isFadingOut = true;
            nextMusicPath = filePath; // Ghim bài nhạc tiếp theo lại, chờ bài cũ tắt hẳn sẽ phát
            return;
        }

        // Nếu không có bài nào đang phát, tiến hành phát bài mới luôn (Fade In từ 0)
        startNewMusic(filePath);
    }

    // Hàm nội bộ để khởi tạo và phát bài nhạc mới
    private void startNewMusic(String filePath) {
        try {
            backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal(filePath));
            backgroundMusic.setLooping(true);

            // Bắt đầu từ âm lượng 0 để tạo hiệu ứng to dần (Fade In)
            currentVolume = 0f;
            backgroundMusic.setVolume(currentVolume);
            backgroundMusic.play();

            currentMusicPath = filePath;
            isFadingOut = false;
            nextMusicPath = "";
        } catch (Exception e) {
            Gdx.app.error("AudioManager", "Không thể tải file nhạc: " + filePath, e);
        }
    }

    /**
     * HÀM QUAN TRỌNG: Bạn CẦN gọi hàm này trong vòng lặp render chính của game
     * để cập nhật âm lượng nhỏ dần/to dần theo thời gian thực.
     * @param deltaTime Thời gian trôi qua giữa các khung hình (Gdx.graphics.getDeltaTime())
     */
    public void update(float deltaTime) {
        if (backgroundMusic == null) return;

        if (isFadingOut) {
            // 1. Xử lý hạ âm lượng nhỏ dần (Fade Out)
            currentVolume -= fadeSpeed * deltaTime;
            if (currentVolume <= 0f) {
                currentVolume = 0f;
                backgroundMusic.setVolume(currentVolume);

                // Sau khi nhạc cũ đã nhỏ hẳn về 0, tiến hành tắt hẳn và giải phóng nó
                stopBackgroundMusicInternal();

                // Nếu có bài nhạc tiếp theo đang đợi, tiến hành phát bài đó lên
                if (!nextMusicPath.isEmpty()) {
                    startNewMusic(nextMusicPath);
                }
            } else {
                backgroundMusic.setVolume(currentVolume);
            }
        } else {
            // 2. Xử lý tăng âm lượng to dần đến mức target (Fade In)
            if (currentVolume < targetVolume) {
                currentVolume += fadeSpeed * deltaTime;
                if (currentVolume > targetVolume) currentVolume = targetVolume;
                backgroundMusic.setVolume(currentVolume);
            }
            // Hoặc giảm nhẹ xuống mức target nếu target thấp hơn âm lượng hiện tại
            else if (currentVolume > targetVolume) {
                currentVolume -= fadeSpeed * deltaTime;
                if (currentVolume < targetVolume) currentVolume = targetVolume;
                backgroundMusic.setVolume(currentVolume);
            }
        }
    }

    /**
     * Dừng nhạc lập tức và kích hoạt hiệu ứng nhỏ dần
     */
    public void fadeOutAndStop() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            isFadingOut = true;
            nextMusicPath = ""; // Dừng hẳn chứ không đổi bài
        }
    }

    private void stopBackgroundMusicInternal() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.dispose();
            backgroundMusic = null;
            currentMusicPath = "";
        }
    }

    public void stopBackgroundMusic() {
        stopBackgroundMusicInternal();
        isFadingOut = false;
        nextMusicPath = "";
    }

    public void pauseBackgroundMusic() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
        }
    }

    public void resumeBackgroundMusic() {
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }
    }

    public float getVolume() {
        return this.currentVolume;
    }

    public void setVolume(float volume) {
        this.targetVolume = volume;
        // Đoạn code chỉnh âm thanh thực tế của bạn
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(volume);
        }
    }

    public void dispose() {
        stopBackgroundMusic();
    }

    /**
     * Hàm phát hiệu ứng âm thanh ngắn (SFX) ngay lập tức khi tương tác puzzle, gạt cần...
     * @param filePath Đường dẫn file âm thanh trong thư mục assets (Ví dụ: "audio/puzzle_wrong.wav")
     * @param volume Âm lượng từ 0.0f (tắt) đến 1.0f (to nhất)
     */
    public void playSoundEffect(String filePath, float volume) {
        try {
            // Nạp file âm thanh ngắn từ thư mục assets vào RAM và phát ngay lập tức không độ trễ
            com.badlogic.gdx.audio.Sound sound = Gdx.audio.newSound(Gdx.files.internal(filePath));
            sound.play(volume);

            // Lưu ý: Đối với các hiệu ứng âm thanh ngắn phát xong rồi thôi, LibGDX sẽ tự giải phóng.
            // Nếu sau này tần suất phát quá nhiều, bạn nên dùng AssetManager để tối ưu quản lý bộ nhớ.
        } catch (Exception e) {
            Gdx.app.error("AudioManager", "Không thể phát hiệu ứng âm thanh: " + filePath, e);
        }
    }
}
