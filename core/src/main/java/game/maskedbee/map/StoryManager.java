package game.maskedbee.map;

import com.badlogic.gdx.*;

public class StoryManager {
    // BIẾN TRẠNG THÁI (Đánh dấu map đã vào)

    private boolean seenNewGameIntro = false;  //Thêm biến trạng thái cho màn chơi đầu tiên
    private boolean seenHoldingIntro = false;
    private boolean seenDisposalIntro = false;
    private boolean seenChapelIntro = false;
    private boolean seenHiddenIntro = false;
    private boolean seenExitIntro = false;

    // TỰ ĐỘNG QUÉT BẢN ĐỒ VÀ KÍCH HOẠT THOẠI
    public void handleExamine(com.badlogic.gdx.math.Rectangle playerHitbox, com.badlogic.gdx.maps.tiled.TiledMap map, DialogueManager dialogueManager, com.badlogic.gdx.utils.Array<game.maskedbee.objects.PushableBlock> pushables, String mapName) {

        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {

            // TẠO VÙNG TƯƠNG TÁC ÁO (Mở rộng thêm 10 pixel xung quanh Player)
            com.badlogic.gdx.math.Rectangle interactRange = new com.badlogic.gdx.math.Rectangle(
                playerHitbox.x - 5, playerHitbox.y - 5,
                playerHitbox.width + 10, playerHitbox.height + 10
            );

            // ƯU TIÊN QUÉT PUSHABLE BLOCKS TRƯỚC
            if (pushables != null) {
                for (game.maskedbee.objects.PushableBlock block : pushables) {
                    // Nếu Player đứng cạnh một khối Pushable
                    if (interactRange.overlaps(block.getBounds())) {

                        // Kiểm tra xem ta đang ở Map nào
                        if (mapName.contains("Disposal")) {
                            // Gọi thẳng sự kiện thoại của Kén Nhỏ
                            checkExamineEvent("disposal_small_cocoon", dialogueManager);
                            return; // Thoát luôn, không quét đồ vật khác nữa
                        }
                    }
                }
            }

            // NẾU KHÔNG CÓ VẬT ĐỘNG NÀO, TIẾP TỤC QUÉT VẬT TĨNH TRÊN TILED
            com.badlogic.gdx.maps.MapLayer examineLayer = map.getLayers().get("ExaminePoints");
            if (examineLayer != null) {
                for (com.badlogic.gdx.maps.MapObject obj : examineLayer.getObjects()) {
                    if (obj instanceof com.badlogic.gdx.maps.objects.RectangleMapObject) {
                        com.badlogic.gdx.math.Rectangle rect = ((com.badlogic.gdx.maps.objects.RectangleMapObject) obj).getRectangle();

                        // Dùng interactRange ảo thay vì playerHitbox để dễ bấm phím hơn
                        if (interactRange.overlaps(rect)) {
                            String objName = obj.getName();
                            if (objName != null && !objName.isEmpty()) {
                                boolean foundExamine = checkExamineEvent(objName, dialogueManager);
                                if (!foundExamine) {
                                    checkExamineEvent(objName, dialogueManager);
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    // Hàm để check newgame -> hiện thoại
    public void checkNewGameIntro(DialogueManager dialogueManager) {
        if (!seenNewGameIntro) {
            String[] introText = {
                "Tại sao mình lại ở nơi này? " +
                    "Phải tìm đường thoát khỏi đây..."
            };
            dialogueManager.startDialogue(introText);
            seenNewGameIntro = true; // Đánh dấu đã xem để không lặp lại
        }
    }

    // KIỂM TRA TƯƠNG TÁC ĐỒ VẬT TĨNH
    // Trả về true nếu có thoại để hiện, false nếu không trúng vật nào
    public boolean checkExamineEvent(String objectName, DialogueManager dialogueManager) {

        // --- MAP 1: COCOON NURSERY ---
        if (objectName.equals("cocoon_player")) {
            String[] text = {
                "Trông nó như một cái kén vậy. "
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("cocoon_other")) {
            String[] text = {
                "Cái gì đó đang giãy giụa đằng sau lớp sáp. Âm thanh như tiếng tim đập."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("wall_honey")) {
            String[] text = {
                "Lớp nhựa chảy ra dày đặc và bốc mùi như mật ong thối. "
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- MAP 2: RITUAL CHAMBER ---
        if (objectName.equals("ritual_table")) {
            String[] text = {
                "Một phiến đá lạnh lẽo, nặng nề."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("ritual_post")) {
            String[] text = {
                "Những cây cột này đủ dày để giấu đi bóng của mình, nhưng chúng chẳng mang lại chút cảm giác an toàn nào."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("ritual_door")) {
            String[] text = {
                "Cửa bị khóa. Mình cần tìm chìa khóa để mở." + "\n" +
                    "Mình có thể nghe thấy tiếng ho vọng ra từ phía bên kia. " +
                    "Có ai đó đang ở trong."
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- MAP 3: HOLDING CHAMBER ---
        if (objectName.equals("holding_skull")) {
            String[] text = {
                "Liệu mình có trở thành một đống xương như thế này không?"
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("holding_special")) {
            String[] text = {
                "Một trang nhật ký nát của ai đó\n" +
                "\"Thứ mùi đó lại luồn qua các khe thông gió khiến chúng tôi ho sặc sụa suốt nhiều giờ liền...",
                "...Lũ lính gác cứ liên tục phát ra những tiếng cạch cạch kỳ dị bên ngoài thanh sắt...",
                    "Bàn tay tôi trông không còn bình thường nữa. Tái nhợt. " +
                    "Các mạch máu nổi rõ và đậm màu hơn trước.\""
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- MAP 4: DISPOSAL PIT ---
        else if (objectName.equals("disposal_small_cocoon")) {
            String[] text = {
                "Mỗi lần đẩy một cái, mình lại nghe thấy tiếng oọc ạch phát ra từ bên trong." + "\n" +
                    "Mình không muốn nghĩ về thứ mà mình đang chạm vào."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("disposal_big_cocoon")) {
            String[] text = {
                "Mình không muốn nghĩ về thứ mà mình đang chạm vào"
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- MAP 4.5.1: OLD CHAPEL ---
        if (objectName.equals("chapel_statue")) {
            String[] text = {
                "Một bức tượng không đầu."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("chapel_coffin")) {
            String[] text = {
                "Chiếc quan tài này đã được niêm phong kiên cố."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("chapel_bookcase1")) {
            String[] text = {
                "Hầu hết các cuốn sách ở đây đã bị dính chặt vào nhau bởi sáp."
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        else if (objectName.equals("chapel_special")) {
            String[] text = {
                "Những trang cuối của cuốn nhật ký dính đầy những vệt sáp nến\n" +
                    "\"Servais nói rằng đau khổ đến từ sự chia cắt...",
                "Đêm nay Servais đưa tôi xuống hầm mộ cũ. " +
                    "Và lần này tôi đã nghe rõ mồn một thứ âm thanh đó. " +
                    "Không phải một giọng nói. Mà là rất nhiều giọng nói.\""
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- MAP 4.5.2: OLD CORRIDOR ---
        if (objectName.equals("corridor_niche")) {
            String[] text = {
                "Mình có cảm giác như mình đang bị quan sát bởi chính những bức tường này."
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- MAP 5: LIBRARY ---
        if (objectName.equals("library_bookcase")) {
            String[] text = {
                "Hầu hết các tựa sách đều bằng một thứ ngôn ngữ kì lạ."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("library_table")) {
            String[] text = {
                "Ai đó đã ngồi đây trong một thời gian rất dài.  Mặt gỗ lốm đốm mực và mật ong.",
                "Có một cái công tắc ở đây."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("library_button")) {
            String[] text = {
                "Mình có thể đẩy quyển sách này."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("library_chest")) {
            String[] text = {
                "Một chiếc rương. Cảm giác nó chứa đựng một điều gì đó rất quan trọng. " + "\n" +
                    "Mình cần tìm cách mở nó."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("library_special")) {
            String[] text = {
                "Một ghi chép bí ẩn với nét chữ viết tay cẩn thận\n" +
                "\"Tình trạng của ong chúa tồi tệ đi theo từng giờ. " +
                    "Lũ ong bắt đầu trở nên bồn chồn mỗi khi nó im lặng... ",
                "Chúng ta đang cạn kiệt thời gian.\n" +
                    "Tôi sẽ không để tất cả những thứ này thối rữa cùng với nó.\n" +
                    "Phải có một vật chứa khác tương thích. Chắc chắn phải có.\""
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- MAP 5.5: HIDDEN ROOM ---
        if (objectName.equals("hidden_niche")) {
            String[] text = {
                "Đầu lâu trông như những sinh vật đang quan sát từ trong bóng tối."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("hidden_coffin")) {
            String[] text = {
                "Một chiếc mặt nạ. " +
                    "Nếu mình đeo thứ này vào liệu chúng có coi mình là đồng loại?"
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("hidden_special")) {
            String[] text = {
                "Một trang nhật ký được kẹp vội bên trong cuốn sách cũ dính đầy sáp nến\n" +
                    "\"Servais đã cảnh cáo không khi ẩm thấp sẽ khiến cơn ho dữ dội hơn, " +
                    "nhưng mình lại thấy không khi nơi đây dễ chịu hơn lạ thường...",
                "Sự tĩnh lặng trên kia khiến mình cảm thấy ngột ngạt. " + "\n" +
                    "Có tiếng động vang lên bên dưới những phiến đá. " +
                    "Nghe nó gần giống như một tiếng ngân nga khiến mình trở nên bình tâm ơn.\""
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- MAP 6: QUEEN CHAMBER ---
        if (objectName.equals("queen_nest")) {
            String[] text = {
                "Trông nó giống như một khối u mọc ra từ nơi này. Có vẻ nó đang chờ đợi một điều gì đó."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("queen_shoot")) {
            String[] text = {
                "Mình có thể cảm nhận được sự chuyển động bên trong. " +
                    "Chúng giống như những quả trứng đang đợi một lý do để nở."
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        else if (objectName.equals("queen_flower")) {
            String[] text = {
                "Sờ vào chúng có cảm giác như làn da ấm ướt. "
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        // --- MAP 6.5: WAX PUMP ---
        if (objectName.equals("wax_tank")) {
            String[] text = {
                "Một bể chứa khổng lồ. " +
                    "Cảm giác như mình đang nhìn vào tinh chất từ tất cả những người đã bị chúng bắt đi."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("wax_pipe")) {
            String[] text = {
                "Những đường ống này đang rung lên mạnh đến mức làm rung chuyển cả mặt đất. " +
                    "Nó đang dẫn thứ chất lỏng đó đến mọi ngóc ngách."
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        else if (objectName.equals("wax_venti")) {
            String[] text = {
                "Luồng khí thoát ra từ khe thông gió này nóng rực. " +
                    "Trông nó giống như một điểm xả áp. Liệu mình có thể phá hủy có máy này?"
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- SPECIAl: EXIT CHAMBER ---
        if (objectName.equals("exit_special")) {
            String[] text = {
                "Một trang giấy với nét chữ viết tay cẩn thận, tao nhã\n" +
                "\"Những kẻ khác gọi nó là kẻ mục nát từ rất lâu...",
                    "Nó đã biết lắng nghe tiếng nói bên dưới thánh đường từ " +
                    "trước khi tôi bắt đầu các bước chuẩn bị. ",
                    "Tôi chỉ đơn giản là chỉ cho nó cách để đáp lại.\""
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        return false; // Không chạm vào đồ vật nào có thoại
    }

    // THOẠI KHI VỪA BƯỚC VÀO MAP MỚI
    public void checkMapEnterEvent(String mapName, DialogueManager dialogueManager) {

        // Holding Chamber
        if (mapName.contains("Holding") && !seenHoldingIntro) {
            String[] story = {
                "Có người ở đây. Mình phải cứu họ."
            };
            dialogueManager.startDialogue(story);
            seenHoldingIntro = true;
        }

        // Disposal
        else if (mapName.contains("Disposal") && !seenDisposalIntro) {
            String[] story = {
                "Thứ đỏ đỏ đằng xa kia trông như 1 chiếc công tắc." + "\n" +
                "Mình phải đẩy những cái kén qua một bên."
            };
            dialogueManager.startDialogue(story);
            seenDisposalIntro = true;
        }
    }
}
