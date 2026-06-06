package game.maskedbee.main;

import java.util.Queue;
import java.util.LinkedList;

public class NotificationManager {
    private static NotificationManager instance;

    public String currentMessage = "";
    public float timer = 0f;
    private static final float DISPLAY_TIME = 2.5f;

    private Queue<String> messageQueue;

    private NotificationManager() {
        messageQueue = new LinkedList<>();
    }

    public static NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    // Hàm gọi thông báo (mặc định hiển thị trong 3.6 giây)
    public void show(String message) {
        messageQueue.add(message);
    }

    // Trừ lùi thời gian
    public void update(float delta) {
        // Nếu ĐANG CÓ thông báo hiển thị trên màn hình
        if (timer > 0) {
            timer -= delta;
            if (timer <= 0) {
                currentMessage = ""; // Hết thời gian thì dọn dẹp chữ trên màn hình
            }
        }
        // Nếu màn hình ĐANG TRỐNG và hàng đợi ĐANG CÓ NGƯỜI CHỜ
        else if (!messageQueue.isEmpty()) {
            currentMessage = messageQueue.poll(); // Rút thông báo tiếp theo ra khỏi hàng đợi
            timer = DISPLAY_TIME; // Bơm lại đầy bình thời gian (3 giây)
        }
    }
}
