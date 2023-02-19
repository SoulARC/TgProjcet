package tel.bot.streampostsbot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import tel.bot.streampostsbot.entity.WorkingGroup;

public interface WorkingGroupDAO extends JpaRepository<WorkingGroup, Long> {
    WorkingGroup findWorkingGroupById(Long id);
}
