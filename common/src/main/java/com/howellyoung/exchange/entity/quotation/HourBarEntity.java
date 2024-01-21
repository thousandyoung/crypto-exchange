package com.howellyoung.exchange.entity.quotation;

import com.howellyoung.exchange.entity.base.BaseBarEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;


/**
 * Store bars of hour.
 */
@Entity
@Table(name = "hour_bars")
public class HourBarEntity extends BaseBarEntity {

}
