package com.howellyoung.exchange.entity.quotation;

import com.howellyoung.exchange.entity.base.BaseBarEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;


/**
 * Store bars of day.
 */
@Entity
@Table(name = "day_bars")
public class DayBarEntity extends BaseBarEntity {

}
