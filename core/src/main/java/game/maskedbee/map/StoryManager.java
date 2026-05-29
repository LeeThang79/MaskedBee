package game.maskedbee.map; // Đổi lại package nếu bạn để ở thư mục khác

public class StoryManager {
    // BIẾN TRẠNG THÁI (Đánh dấu map đã vào)
    private boolean seenHoldingIntro = false;
    private boolean seenDisposalIntro = false;
    private boolean seenChapelIntro = false;
    private boolean seenHiddenIntro = false;
    private boolean seenExitIntro = false;

    // 1. THOẠI KHI VỪA BƯỚC VÀO MAP MỚI
    public void checkMapEnterEvent(String mapName, DialogueManager dialogueManager) {

        // Old Chapel
        if (mapName.contains("Chapel") && !seenChapelIntro) {
            String[] story = {
                "Seraph đã cảnh cáo không khi ẩm thấp sẽ khiến cơn ho dữ dội hơn, nhưng mình lại thấy không khi nơi đây dễ chịu hơn lạ thường.",
                "Sự tĩnh lặng trên kia khiến mình cam thấy ngột ngạt. những lời nguyện vang vọng, tiếng bước chân nghe that xa cách.",
                "Đôi lúc mình nghĩ tu viện đã bị bỏ hoang rất lâu trước khi mọi người ra đi. \n" +
                    "\n" +
                    "Có tiếng động gì đó vang lên bên dưới những phiến đá. Nghe nó gần giống như một tiếng ngân nga.",
                "Khi ngồi gần lối cầu thang cũ, thứ âm thanh đó lại khiến mình bình tâm. \n" +
                    "\n" +
                    "Thật không thể hiêu nổi. "
            };
            dialogueManager.startDialogue(story);
            seenChapelIntro = true;
        }

        // Hidden Room
        if (mapName.contains("Hidden") && !seenHiddenIntro) {
            String[] story = {
                "Seraph nói rằng đau khổ đến từ sự chia cắt. \n" +
                    "\n" +
                    "Khỏi chúa, đồng loại và chính bản than ta. ",
                "Lúc đầu tôi đã tranh cãi với anh ta... Anh ta bật cười ... Không phải nụ cười độc ác. Mà giống như nụ cười dành cho một đứa trẻ cứng đầu... ",
                "Đêm nay Seraph đưa tôi xuống hầm mộ cũ. \n" +
                    "\n" +
                    "Và lần này tôi đã nghe rõ mồn một thứ âm thanh đó. \n" +
                    "\n" +
                    "Không phải một giọng nói. Mà là rất nhiều giọng nói. "
            };
            dialogueManager.startDialogue(story);
            seenHiddenIntro = true;
        }

        // Exit Chamber
        if (mapName.contains("Exit") && !seenExitIntro) {
            String[] story = {
                "Những kẻ khác gọi nó là kẻ mục nát từ rất lâu trước khi tôi tìm thấy nó. ",
                "Nó đã biết lắng nghe tiếng nói bên dưới thánh đường từ trước khi tôi bắt đầu các bước chuẩn bị.",
                "Tôi chỉ đơn giản là chỉ cho nó cách để đáp lại."
            };
            dialogueManager.startDialogue(story);
            seenExitIntro = true;
        }

        // Holding Chamber
        if (mapName.contains("Holding") && !seenHoldingIntro) {
            String[] story = {
                "Thứ mùi đó lại luồn qua các khe thông gió.",
                "Tôi có thể ngửi thấy nó trước cả khi nó tràn vào các buồng giam. Ban đầu là vị ngọt lịm, sau đó chuyển thành vị đắng ngắt găm lại nơi cuống họng suốt nhiều giờ liền.",
                "Vài người ở gần bức tường phía sau đã hoàn toàn không còn phản ứng gì với nó nữa. Họ chỉ ngồi đực ra đó, trân trân nhìn xuống sàn nhà trong khi những người còn lại như chúng tôi ho sặc sụa.",
                "Lũ lính gác cứ liên tục phát ra những tiếng cạch cạch kỳ dị bên ngoài thanh sắt.",
                "Bàn tay tôi trông không còn bình thường nữa. Tái nhợt. Các mạch máu nổi rõ và đậm màu hơn trước."
            };
            dialogueManager.startDialogue(story);
            seenHoldingIntro = true;
        }

        // Disposal
        else if (mapName.contains("Disposal") && !seenDisposalIntro) {
            String[] story = {
                "Nhung cai ken nay vua nang vua lanh leo.",
                "Toi phai day chung qua mot ben...",
                "Nhung toi so cai thu gi do co the ri ra tu ben trong."
            };
            dialogueManager.startDialogue(story);
            seenDisposalIntro = true;
        }
    }

    // 2. THOẠI KHI TƯƠNG TÁC ĐỒ VẬT (BẤM PHÍM E)
    public void checkInteractEvent(String objectName, DialogueManager dialogueManager) {

        // NHỮNG MẢNH NHẬT KÝ (LORE CỐT TRUYỆN)
        if (objectName.equals("diary_old_chapel")) {
            String[] lore = {
                "[Mot trang nhat ky duoc kep voi ben trong cuon sach...]",
                "Seraph da canh cao khong khi am thap se khien con ho du doi hon.",
                "Nhung minh lai thay khong khi noi day de chiu hon la thuong.",
                "Su tinh lang tren kia khien minh thay ngot ngat...",
                "Nhung loi nguyen vang vong, tieng buoc chan nghe that xa cach.",
                "Co tieng dong gi do vang len ben duoi nhung phien da.",
                "Nghe no gan giong nhu mot tieng ngan nga...",
                "Khi ngoi gan cau thang cu, thu am thanh do lai khien minh binh tam."
            };
            dialogueManager.startDialogue(lore);
        }
        else if (objectName.equals("diary_hidden_room")) {
            String[] lore = {
                "[Nhung trang cuoi cung, dinh day vet sap nen...]",
                "Seraph noi rang dau kho den tu su chia cat.",
                "Khoi chua, dong loai va chinh ban than ta.",
                "Luc dau toi da tranh cai voi anh ta... Anh ta bat cuoi...",
                "Dem nay Seraph dua toi xuong ham mo cu.",
                "Va lan nay toi da nghe ro mon mot thu am thanh do.",
                "Khong phai mot giong noi. Ma la rat nhieu giong noi."
            };
            dialogueManager.startDialogue(lore);
        }
        else if (objectName.equals("diary_holding")) {
            String[] lore = {
                "[Nhat ky cua Tu nhan]",
                "Thu mui do lai luon qua cac khe thong gio.",
                "Ban dau la vi ngot lim, sau do chuyen thanh vi dang ngat.",
                "Vai nguoi o gan buc tuong da hoan toan khong con phan ung gi nua.",
                "Ho chi ngoi duc ra do, tran tran nhin xuong san nha...",
                "Ban tay toi trong khong con binh thuong nua. Tai nhot.",
                "Cac mach mau noi ro va dam mau hon truoc."
            };
            dialogueManager.startDialogue(lore);
        }

        // TƯƠNG TÁC MÔI TRƯỜNG (MÔ TẢ CẢNH VẬT)

        // -- Map Cocoon Nursery --
        else if (objectName.equals("player_cocoon")) {
            String[] text = {
                "No van con am.",
                "Ben trong phu mot lop dich nhay dac quanh...",
                "Nong nac mui ngot lim khien toi noi het ca da ga."
            };
            dialogueManager.startDialogue(text);
        }
        else if (objectName.equals("other_cocoons")) {
            String[] text = {
                "Cai gi do dang giay giua dang sau lop sap.",
                "Trong no khong con la con nguoi nua roi."
            };
            dialogueManager.startDialogue(text);
        }

        // -- Map Ritual Chamber --
        else if (objectName.equals("ritual_altar")) {
            String[] text = {
                "Mot phien da lanh leo, nang ne.",
                "Co nhung ranh mo duoc khac sau vao be mat...",
                "Dung de dan mot thu chat long nao do chay di.",
                "Nhin vao no thoi cung du lam toi non nao."
            };
            dialogueManager.startDialogue(text);
        }
        else if (objectName.equals("locked_door_voice")) {
            String[] text = {
                "O khoa nang ne va ri set bam chat.",
                "Toi co the nghe thay tieng ho nghen va xich sat loang xoang.",
                "Co ai do dang o trong..."
            };
            dialogueManager.startDialogue(text);
        }

        // -- Map Library --
        else if (objectName.equals("desk_library")) {
            String[] text = {
                "Ai do da ngoi day trong mot thoi gian rat dai.",
                "Mat go bi hoen o boi muc va... mat ong mau vang."
            };
            dialogueManager.startDialogue(text);
        }
    }

    // 3. HÀM HIỂN THỊ KẾT THÚC (ENDINGS)
    public void showEnding(int endingID, DialogueManager dialogueManager) {
        if (endingID == 1) {
            String[] text = {
                "ENDING 1: SU TRON CHAY MU QUANG",
                "Toi da khong quay dau lai va toi khong tim kiem cau tra loi.",
                "Khong khi lanh gia cua vung nui thieu dot la phoi toi...",
                "Nhung no van tot hon thu mui ngot lim ngot ngat kia.",
                "Toi tu do roi...",
                "Vay tai sao trong nhung giac mo, toi van nghe thay tieng ngan nga ay?"
            };
            dialogueManager.startDialogue(text);
        }
        else if (endingID == 2) {
            String[] text = {
                "ENDING 2: GIU LAI NHAN TINH",
                "Toi da thay a. Toi da thay su muc nat dang sau lop vang kim ay.",
                "Chung trao cho toi su vinh cuu...",
                "Nhung toi lai chon lay tam than xac pham dang thoi rua nay.",
                "To the dang gao thet phia sau toi...",
                "Toi lai co doc mot lan nua. Va lan dau tien, cam giac nhu mot chien thang."
            };
            dialogueManager.startDialogue(text);
        }
        // Thêm Endings 3, 4, 5, 6 tương tự ở đây...
    }
}
