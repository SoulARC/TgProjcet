package tel.bot.streampostsbot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import tel.bot.streampostsbot.entity.Hashtag;
import tel.bot.streampostsbot.entity.MainChannel;

import java.util.List;

public interface HashtagDAO extends JpaRepository<Hashtag, Long> {
    List<Hashtag> getHashtagsByMainChannel(MainChannel mainChannel);
    Hashtag getHashtagsById(Long id);
    void deleteAllByMainChannel(MainChannel mainChannel);
}
