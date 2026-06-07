package game.maskedbee.map;

import java.util.HashMap;
import java.util.Map;

public class DialogueLibrary {
    // Từ điển lưu trữ thoại: Key là objectName, Value là mảng String[]
    private static final Map<String, String[]> examineDialogues = new HashMap<>();
    private static final Map<String, String[]> introDialogues = new HashMap<>();

    static {
        // --- MAP 1: COCOON NURSERY ---
        examineDialogues.put("cocoon_player", new String[]{"Trông nó như một cái kén vậy."});
        examineDialogues.put("cocoon_other", new String[]{"Cái gì đó đang giãy giụa đằng sau lớp sáp. Âm thanh như tiếng tim đập."});
        examineDialogues.put("wall_honey", new String[]{"Lớp nhựa chảy ra dày đặc và bốc mùi như mật ong thối."});

        // --- MAP 2: RITUAL CHAMBER ---
        examineDialogues.put("ritual_table",new String[]{"Một phiến đá lạnh lẽo, nặng nề."});
        examineDialogues.put("ritual_post",new String[]{"Những cây cột này đủ dày để giấu đi bóng của mình, nhưng chúng chẳng mang lại chút cảm giác an toàn nào."});
        examineDialogues.put("ritual_door", new String[]{
            "Cửa bị khóa. Mình cần tìm chìa khóa để mở.\n"+
            "Mình có thể nghe thấy tiếng ho vọng ra từ phía bên kia." + "Có ai đó đang ở trong."
        });

        // --- MAP 3: HOLDING CHAMBER ---
        examineDialogues.put("holding_skull", new String[]{"Liệu mình có trở thành một đống xương như thế này không?"});
        examineDialogues.put("holding_special", new String[]{
            "Một trang nhật ký nát của ai đó\n\"Thứ mùi đó lại luồn qua các khe thông gió khiến chúng tôi ho sặc sụa suốt nhiều giờ liền...",
            "...Lũ lính gác cứ liên tục phát ra những tiếng cạch cạch kỳ dị bên ngoài thanh sắt...",
            "Bàn tay tôi trông không còn bình thường nữa. Tái nhợt. Các mạch máu nổi rõ và đậm màu hơn trước.\""
        });

        // --- MAP 4: DISPOSAL PIT ---
        examineDialogues.put("disposal_small_cocoon", new String[]{
            "Mỗi lần đẩy một cái, mình lại nghe thấy tiếng oọc ạch phát ra từ bên trong.\nMình không muốn nghĩ về thứ mà mình đang chạm vào."
        });
        examineDialogues.put("disposal_big_cocoon", new String[]{
            "Mình không muốn nghĩ về thứ mà mình đang chạm vào"
        });

        // --- MAP 4.5.1: OLD CHAPEL ---
        examineDialogues.put("chapel_statue", new String[]{
            "Một bức tượng không đầu."
        });
        examineDialogues.put("chapel_coffin", new String[]{
            "Chiếc quan tài này đã được niêm phong kiên cố."
        });
        examineDialogues.put("chapel_bookcase1", new String[]{
            "Hầu hết các cuốn sách ở đây đã bị dính chặt vào nhau bởi sáp."
        });
        examineDialogues.put("chapel_special", new String[]{
            "Những trang cuối của cuốn nhật ký dính đầy những vệt sáp nến\n\"Servais nói rằng đau khổ đến từ sự chia cắt...",
            "Đêm nay Servais đưa tôi xuống hầm mộ cũ. Và lần này tôi đã nghe rõ mồn một thứ âm thanh đó. Không phải một giọng nói. Mà là rất nhiều giọng nói.\""
        });

        // --- MAP 4.5.2: OLD CORRIDOR ---
        examineDialogues.put("corridor_niche", new String[]{
            "Mình có cảm giác như mình đang bị quan sát bởi chính những bức tường này."
        });

        // --- MAP 5: LIBRARY ---
        examineDialogues.put("library_bookcase", new String[]{
            "Hầu hết các tựa sách đều bằng một thứ ngôn ngữ kì lạ."
        });
        examineDialogues.put("library_table", new String[]{
            "Ai đó đã ngồi đây trong một thời gian rất dài. Mặt gỗ lốm đốm mực và mật ong.",
            "Có một cái công tắc ở đây."
        });
        examineDialogues.put("library_button", new String[]{
            "Mình có thể đẩy quyển sách này."
        });
        examineDialogues.put("library_chest", new String[]{
            "Một chiếc rương. Cảm giác nó chứa đựng một điều gì đó rất quan trọng. \nMình cần tìm cách mở nó."
        });
        examineDialogues.put("library_special", new String[]{
            "Một ghi chép bí ẩn với nét chữ viết tay cẩn thận\n\"Tình trạng của ong chúa tồi tệ đi theo từng giờ. Lũ ong bắt đầu trở nên bồn chồn mỗi khi nó im lặng... ",
            "Chúng ta đang cạn kiệt thời gian.\nTôi sẽ không để tất cả những thứ này thối rữa cùng với nó.\nPhải có một vật chứa khác tương thích. Chắc chắn phải có.\""
        });

        // --- MAP 5.5: HIDDEN ROOM ---
        examineDialogues.put("hidden_niche", new String[]{
            "Đầu lâu trông như những sinh vật đang quan sát từ trong bóng tối."
        });
        examineDialogues.put("hidden_coffin", new String[]{
            "Một chiếc mặt nạ. Nếu mình đeo thứ này vào liệu chúng có coi mình là đồng loại?"
        });
        examineDialogues.put("hidden_special", new String[]{
            "Một trang nhật ký được kẹp vội bên trong cuốn sách cũ dính đầy sáp nến",
            "\"Servais đã cảnh cáo không khi ẩm thấp sẽ khiến cơn ho dữ dội hơn, nhưng mình lại thấy không khi nơi đây dễ chịu hơn lạ thường...",
            "Sự tĩnh lặng trên kia khiến mình cảm thấy ngột ngạt. \nCó tiếng động vang lên bên dưới những phiến đá. ",
            "Nghe nó gần giống như một tiếng ngân nga khiến mình trở nên bình tâm ơn.\""
        });

        // --- MAP 6: QUEEN CHAMBER ---
        examineDialogues.put("queen_nest", new String[]{
            "Trông nó giống như một khối u mọc ra từ nơi này. Có vẻ nó đang chờ đợi một điều gì đó."
        });
        examineDialogues.put("queen_shoot", new String[]{
            "Mình có thể cảm nhận được sự chuyển động bên trong. Chúng giống như những quả trứng đang đợi một lý do để nở."
        });
        examineDialogues.put("queen_flower", new String[]{
            "Sờ vào chúng có cảm giác như làn da ấm ướt. "
        });

        // --- MAP 6.5: WAX PUMP ---
        examineDialogues.put("wax_tank", new String[]{
            "Một bể chứa khổng lồ. Cảm giác như mình đang nhìn vào tinh chất từ tất cả những người đã bị chúng bắt đi."
        });
        examineDialogues.put("wax_pipe", new String[]{
            "Những đường ống này đang rung lên mạnh đến mức làm rung chuyển cả mặt đất. Nó đang dẫn thứ chất lỏng đó đến mọi ngóc ngách."
        });
        examineDialogues.put("wax_venti", new String[]{
            "Luồng khí thoát ra từ khe thông gió này nóng rực. Trông nó giống như một điểm xả áp. Liệu mình có thể phá hủy có máy này?"
        });

        // --- SPECIAL: EXIT CHAMBER ---
        examineDialogues.put("exit_special", new String[]{
            "Một trang giấy với nét chữ viết tay cẩn thận, tao nhã\n\"Những kẻ khác gọi nó là kẻ mục nát từ rất lâu...",
            "Nó đã biết lắng nghe tiếng nói bên dưới thánh đường từ trước khi tôi bắt đầu các bước chuẩn bị. ",
            "Tôi chỉ đơn giản là chỉ cho nó cách để đáp lại.\\\""
        });


        // --- HỘI THOẠI MỞ ĐẦU MAP (INTRO) ---
        introDialogues.put("new_game", new String[]{"Tại sao mình lại ở nơi này? Phải tìm đường thoát khỏi đây..."});
        introDialogues.put("Holding", new String[]{"Có người ở đây. Mình phải cứu họ."});
        introDialogues.put("Disposal", new String[]{"Thứ đỏ đỏ đằng xa kia trông như 1 chiếc công tắc.\nMình phải đẩy những cái kén qua một bên."});
    }

    // Hàm lấy thoại Examine
    public static String[] getExamineText(String objectName) {
        return examineDialogues.get(objectName);
    }

    // Hàm lấy thoại Intro
    public static String[] getIntroText(String mapName) {
        return introDialogues.get(mapName);
    }
}
