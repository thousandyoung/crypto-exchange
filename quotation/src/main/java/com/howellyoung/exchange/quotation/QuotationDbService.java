package com.howellyoung.exchange.quotation;

import java.util.List;

import com.howellyoung.exchange.entity.quotation.*;
import com.howellyoung.exchange.repository.quotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@Transactional
public class QuotationDbService {

    @Autowired
    private SecBarEntityRepository secBarEntityRepository;

    @Autowired
    private MinBarEntityRepository minBarEntityRepository;

    @Autowired
    private HourBarEntityRepository hourBarEntityRepository;

    @Autowired
    private DayBarEntityRepository dayBarEntityRepository;

    @Autowired
    private TickEntityRepository tickEntityRepository;

    public void saveBars(SecBarEntity sec, MinBarEntity min, HourBarEntity hour, DayBarEntity day) {
        if (sec != null) {
            secBarEntityRepository.saveIgnore(sec);
        }
        if (min != null) {
            minBarEntityRepository.saveIgnore(min);
        }
        if (hour != null) {
            hourBarEntityRepository.saveIgnore(hour);
        }
        if (day != null) {
            dayBarEntityRepository.saveIgnore(day);
        }
    }

    public void saveTicks(List<TickEntity> ticks) {
        for (TickEntity tick : ticks) {
            tickEntityRepository.saveIgnore(tick);
        }
    }
}
