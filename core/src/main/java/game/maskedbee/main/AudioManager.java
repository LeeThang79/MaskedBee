package game.maskedbee.main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

public class AudioManager {
    private static AudioManager instance;

    private Music backgroundMusic;
    private String currentMusicPath = "";

    // Các biến phục vụ tính năng Fade
    private float targetVolume = 0.5f;     // Âm lượng đích mong muốn
    private float currentVolume = 0.5f;    // Âm lượng hiện tại đang phát
    private float fadeSpeed = 1.5f;        // Tốc độ fade

    private boolean isFadingOut = false;   // Cờ đánh dấu đang nhỏ dần để tắt
    private String nextMusicPath = "";     // Lưu bài nhạc tiếp theo sẽ phát sau khi bài cũ tắt hẳn

    private AudioManager() {}

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    //Hàm phát nhạc nền
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

    //Dừng nhạc lập tức và kích hoạt hiệu ứng nhỏ dần
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
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(volume);
        }
    }

    public void dispose() {
        stopBackgroundMusic();
    }

    //Hàm phát hiệu ứng âm thanh ngắn (SFX) ngay lập tức
    public void playSoundEffect(String filePath, float volume) {
        try {
            com.badlogic.gdx.audio.Sound sound = Gdx.audio.newSound(Gdx.files.internal(filePath));
            sound.play(volume);

        } catch (Exception e) {
            Gdx.app.error("AudioManager", "Không thể phát hiệu ứng âm thanh: " + filePath, e);
        }
    }
}
