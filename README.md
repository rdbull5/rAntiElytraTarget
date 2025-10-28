https://discord.gg/UyTwKtj25Z

# rAntiElytraTarget;

Pluginin amacı elytra target hilesini kullananları yakalamak üzerinedir. Config dosyasından her şey değiştirilebilir.

Log veren oyuncuları kontrole çekmenize gerek yoktur eğer gerçekten Elytra Target kullanıyorlarlarsa otomatik olarak banlanırlar fakat üst üste 2-3 kere 4x-5x log verirlerse kontrole çekebilirsiniz.

Bu plugin tamamen ücretsizdir fakat isminin değiştirilmesi, kopyalanması, parayla satılması kesinlikle yasaktır tespit edildiği taktirde gerekli işlemler başlatılacaktır.
İletişim için discord: @rdbull.

Authors: @rdbull. & @rest.d

# Config
max-logs: 7 # Oyuncu kaç log verince işlem uygulansın?
log-reset-seconds: 15 # Oyuncunun logu kaç saniyede bir sıfırlansın?
alert-message: "&c&l[Uyarı] &e%player% &6Elytra target şüphesi! &7[x%count%]" # Şüphe mesajı.
ban-command: "ban %player% %reason%" # Hangı komutla işlem uygulansın?
ban-reason: "Elytra target tespit edildi." # Hangi sebeple banlansın?

# Commands:
/rantielytratarget reload

# Permissions:
rantielytratarget.alert
Gelen elytra target loglarını görme yetkisi.

rantielytratarget.reload
Configi yeniden yükleme yetkisi.
