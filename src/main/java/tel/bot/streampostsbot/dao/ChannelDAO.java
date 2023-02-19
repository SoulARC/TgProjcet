package tel.bot.streampostsbot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import tel.bot.streampostsbot.entity.Channel;

public interface ChannelDAO extends JpaRepository<Channel, Long> {
    Channel findChannelByChannelId(Long id);
    Channel findChannelById(Long id);
}
