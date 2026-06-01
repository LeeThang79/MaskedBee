package game.maskedbee.map;

import com.badlogic.gdx.*;

public class StoryManager {
    // BIẾN TRẠNG THÁI (Đánh dấu map đã vào)
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

    // KIỂM TRA TƯƠNG TÁC ĐỒ VẬT TĨNH
    // Trả về true nếu có thoại để hiện, false nếu không trúng vật nào
    public boolean checkExamineEvent(String objectName, DialogueManager dialogueManager) {

        // --- MAP 1: COCOON NURSERY ---
        if (objectName.equals("cocoon_player")) {
            String[] text = {
                "Nó vẫn còn ấm. " + "\n" +
                    "Bên trong phủ một lớp dịch nhầy đặc quánh " + "\n" +
                    "nồng nặc mùi ngọt lịm khiến tôi nổi hết cả da gà."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("cocoon_other")) {
            String[] text = {
                "Cái gì đó đang giãy giụa đằng sau lớp sáp. " + "\n" +
                    "Trông nó không còn là con người nữa rồi."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("wall_honey")) {
            String[] text = {
                "Lớp nhựa chảy ra dày đặc và bốc mùi như mật ong thối. " + "\n" +
                    "Nó nhỏ giọt chậm chạp, " + "\n" +
                    "nhưng cảm giác như nó đang nuốt chửng cả căn phòng này."
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- MAP 2: RITUAL CHAMBER ---
        if (objectName.equals("ritual_table")) {
            String[] text = {
                "Một phiến đá lạnh lẽo, nặng nề. Dùng để dẫn một thứ" + "\n" +
                    "chất lỏng nào đó chảy đi." + "\n" +
                    "Có những rãnh mờ được khắc sâu vào bề mặt..." + "\n" +
                    "dùng để dẫn một thứ chất lỏng nào đó chảy đi. " + "\n" +
                    "Nhìn vào nó thôi cũng đủ làm tôi nôn nao."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("ritual_post")) {
            String[] text = {
                "Những cây cột này đủ dày để giấu đi bóng của tôi, " + "\n" +
                    "nhưng chúng chẳng mang lại chút cảm giác an toàn nào."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("ritual_door")) {
            String[] text = {
                "Ổ khóa nặng nề và rỉ sét bám chặt.  " + "\n" +
                    "Tôi có thể nghe thấy tiếng ho nghẹn và tiếng xích sắt " + "\n" +
                    "loảng xoảng vọng ra từ phía bên kia. Có ai đó đang ở trong..."
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- MAP 3: HOLDING CHAMBER ---
        if (objectName.equals("holding_skull")) {
            String[] text = {
                "Liệu rồi mình có kết thúc bằng một" + "\n" +
                    "đống xương tàn như thế này không?"
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("holding_vent")) {
            String[] text = {
                "Lại là cái mùi đó. " + "\n" +
                    "Ngọt đến ngột ngạt khiến đầu óc tôi quay cuồng."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("holding_special")) {
            String[] text = {
                "Thứ mùi đó lại luồn qua các khe thông gió." + "\n" +
                    "Tôi có thể ngửi thấy nó trước cả khi nó tràn vào các buồng giam." + "\n" +
                    "Ban đầu là vị ngọt lịm, sau đó chuyển thành " + "\n" +
                    "vị đắng ngắt găm lại nơi cuống họng suốt nhiều giờ liền. ",
                "Vài người ở gần bức tường phía sau đã hoàn toàn" + "\n" +
                    "không còn phản ứng gì với nó nữa." + "\n" +
                    "Họ chỉ ngồi đực ra đó, trân trân nhìn xuống sàn nhà trong khi " + "\n" +
                    "những người còn lại như chúng tôi ho sặc sụa.",
                "Lũ lính gác cứ liên tục phát ra những tiếng cạch cạch" + "\n" +
                    "kỳ dị bên ngoài thanh sắt." + "\n" +
                    "Bàn tay tôi trông không còn bình thường nữa. Tái nhợt." + "\n" +
                    "Các mạch máu nổi rõ và đậm màu hơn trước."
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- MAP 4: DISPOSAL PIT ---
        else if (objectName.equals("disposal_small_cocoon")) {
            String[] text = {
                "Mỗi lần đẩy một cái, tôi lại nghe thấy tiếng oọc ạch" + "\n" +
                    "nhớt phát ra từ bên trong." + "\n" +
                    "Tôi không muốn nghĩ về thứ mà mình đang chạm vào nữa."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("disposal_big_cocoon")) {
            String[] text = {
                "Chúng nặng hơn vẻ bề ngoài nhiều." + "\n" +
                "Cảm giác... rắn chắc, " + "\n" +
                    "nhưng ẩn sâu bên dưới là một sự mềm nhũn đến phát tởm."
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- MAP 4.5.1: OLD CHAPEL ---
        if (objectName.equals("chapel_statue")) {
            String[] text = {
                "Một bức tượng không đầu. Lớp nhựa chạm vào vẫn còn ấm."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("chapel_coffin")) {
            String[] text = {
                "Cỗ quan tài này đã bị niêm phong kiên cố."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("chapel_bookcase1")) {
            String[] text = {
                "Hầu hết các cuốn sách ở đây đã bị..." + "\n" +
                    "dính chặt vào nhau bởi một thứ chất lỏng."
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        else if (objectName.equals("chapel_special")) {
            String[] text = {
                "Seraph đã cảnh cáo không khi ẩm thấp sẽ khiến " +
                    "cơn ho dữ dội hơn, nhưng mình lại thấy " + "\n" +
                    "không khi nơi đây dễ chịu hơn lạ thường.",
                "Sự tĩnh lặng trên kia khiến mình cam thấy ngột ngạt." + "\n" +
                    "những lời nguyện vang vọng, tiếng bước chân nghe that xa cách.",
                "Đôi lúc mình nghĩ tu viện đã bị " + "\n" +
                    "bỏ hoang rất lâu trước khi mọi người ra đi." + "\n" +
                    "Có tiếng động gì đó vang lên bên dưới những phiến đá. " + "\n" +
                    "Nghe nó gần giống như một tiếng ngân nga.",
                "Khi ngồi gần lối cầu thang cũ, thứ âm thanh đó lại khiến mình bình tâm.\n" +
                    "Thật không thể hiêu nổi. "
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- MAP 4.5.2: OLD CORRIDOR ---
        if (objectName.equals("corridor_niche")) {
            String[] text = {
                "Những hốc trống hoác trên vách đá. " + "\n" +
                    "Tôi có cảm giác như mình đang bị quan sát bởi " + "\n" +
                    "chính những bức tường này."
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- MAP 5: LIBRARY ---
        if (objectName.equals("library_bookcase")) {
            String[] text = {
                "Hết hàng này đến hàng khác toàn là những ghi chép cổ. " + "\n" +
                    "Hầu hết các tựa sách đều bằng " + "\n" +
                    "một thứ ngôn ngữ mà tôi không hề biết."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("library_table")) {
            String[] text = {
                "Ai đó đã ngồi đây trong một thời gian rất dài. " + "\n" +
                    "Mặt gỗ bị hoen ố bởi mực và... mật ong màu vàng."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("library_chest")) {
            String[] text = {
                "Một chiếc rương nẹp sắt nặng nề. " + "\n" +
                    "Cảm giác nó chứa đựng một điều gì đó rất quan trọng."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("library_special")) {
            String[] text = {
                "Tình trạng của nó tồi tệ đi theo từng giờ. \n" +
                    "Lũ ong thợ bắt đầu trở nên bồn chồn mỗi khi nó im lặng. " + "\n" +
                    "Toàn bộ các khuôn viên sẽ ngừng phản hồi cho đến khi " + "\n" +
                    "nhịp thở của nó trở lại. " + "\n" +
                    "Đàn trùng đang cảm nhận được sự suy yếu.",
                "Chúng ta đang cạn kiệt thời gian." + "\n" +
                    "Tôi sẽ không để tất cả những thứ này thối rữa cùng với nó." + "\n" +
                    "Phải có một vật chứa khác tương thích. Chắc chắn phải có."
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- MAP 5.5: HIDDEN ROOM ---
        if (objectName.equals("hidden_niche")) {
            String[] text = {
                "Hàng tá sinh vật..." + "\n" +
                    "đang chực chờ quan sát từ trong bóng tối của những phiến đá."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("hidden_coffin")) {
            String[] text = {
                "Một chiếc mặt nạ.  " + "\n" +
                    "Có một mùi hương thoang thoảng bên trong " + "\n" +
                    "khiến nhịp tim tôi chậm lại." + "\n" +
                    "Nếu tôi đeo thứ này vào..." + "\n" +
                    "liệu chúng có coi tôi là đồng loại không?"
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("hidden_special")) {
            String[] text = {
                "Seraph nói rằng đau khổ đến từ sự chia cắt. \n" +
                    "Khỏi chúa, đồng loại và chính bản than ta. ",
                "Lúc đầu tôi đã tranh cãi với anh ta... Anh ta bật cười " + "\n" +
                    "... Không phải nụ cười độc ác." + "\n" +
                    " Mà giống như nụ cười dành cho một đứa trẻ cứng đầu...",
                "Đêm nay Seraph đưa tôi xuống hầm mộ cũ.\n" +
                    "Và lần này tôi đã nghe rõ mồn một thứ âm thanh đó.\n" +
                    "Không phải một giọng nói. Mà là rất nhiều giọng nói."
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- MAP 6: QUEEN CHAMBER ---
        if (objectName.equals("queen_nest")) {
            String[] text = {
                "Đó là một ngọn núi hổ phách bán trong suốt đang phập phồng run rẩy." + "\n" +
                    "Trông nó không giống một kiến trúc..." + "\n" +
                    "nó giống như một khối u mọc ra từ móng của tu viện." + "\n" +
                    "Mùi mật ong lên men nồng nặc đến mức đốt cháy cả cuống họng tôi."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("queen_shoot")) {
            String[] text = {
                "Tôi có thể cảm nhận được sự chuyển động bên trong..." + "\n" +
                    "một nhịp đập điên cuồng, dồn dập" + "\n" +
                    "Chúng giống như những quả trứng đang đợi một lý do để nở."
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        else if (objectName.equals("queen_flower")) {
            String[] text = {
                "Sờ vào chúng có cảm giác như làn da ấm ướt. " + "\n" +
                    "Khi tôi chạm vào, toàn bộ bông hoa rùng mình..." + "\n" +
                    "và một đám bụi ngọt lịm, ngột ngạt bung tỏa ra."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        // --- MAP 6.5: WAX PUMP ---
        if (objectName.equals("wax_tank")) {
            String[] text = {
                "Một bể chứa khổng lồ đầy sáp nóng chảy. " + "\n" +
                    "Nó đang sủi bọt và cuộn xoáy tựa như có sự sống riêng." + "\n" +
                    "Cảm giác như tôi đang nhìn vào phần tinh chất đậm đặc..." + "\n" +
                    "của tất cả những người đã bị chúng bắt đi."
            };
            dialogueManager.startDialogue(text);
            return true;
        }
        else if (objectName.equals("wax_pipe")) {
            String[] text = {
                "Những đường ống này đang rung lên mạnh đến mức" + "\n" +
                    "làm rung chuyển cả ván sàn." + "\n" +
                    "Nó đang dẫn thứ chất độc màu vàng kim đó đến mọi ngóc ngách."
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        else if (objectName.equals("wax_venti")) {
            String[] text = {
                "Luồng khí thoát ra từ khe thông gió này nóng rực..." + "\n" +
                    "nồng nặc mùi đường cháy và mùi sắt." + "\n" +
                    "Trông nó giống như một điểm xả áp."
            };
            dialogueManager.startDialogue(text);
            return true;
        }

        // --- SPECIAl: EXIT CHAMBER ---
        if (objectName.equals("exit_special")) {
            String[] text = {
                "Những kẻ khác gọi nó là kẻ mục nát từ rất lâu " + "\n" +
                    "trước khi tôi tìm thấy nó. " + "\n" +
                    "Nó đã biết lắng nghe tiếng nói bên dưới thánh đường từ " + "\n" +
                    "trước khi tôi bắt đầu các bước chuẩn bị. " + "\n" +
                    "Tôi chỉ đơn giản là chỉ cho nó cách để đáp lại."
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
                "Có người ở đây. Tôi phải cứu họ."
            };
            dialogueManager.startDialogue(story);
            seenHoldingIntro = true;
        }

        // Disposal
        else if (mapName.contains("Disposal") && !seenDisposalIntro) {
            String[] story = {
                "Những cái kén này vừa nặng vừa lạnh lẽo." + "\n" +
                "Tôi phải đẩy chúng qua một bên," + "\n" +
                "nhưng tôi sợ cái thứ gì đó có thể rỉ ra từ bên trong."
            };
            dialogueManager.startDialogue(story);
            seenDisposalIntro = true;
        }
    }
}
