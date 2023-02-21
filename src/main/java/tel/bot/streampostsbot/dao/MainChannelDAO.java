package tel.bot.streampostsbot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import tel.bot.streampostsbot.entity.MainChannel;

public interface MainChannelDAO extends JpaRepository<MainChannel, Long> {
    MainChannel findMainChannelById(Long id);

}
