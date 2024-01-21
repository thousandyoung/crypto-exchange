package com.howellyoung.exchange.entity.quotation;

import com.howellyoung.exchange.entity.base.BaseBarEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Store bars of second.
 */
@Entity
@Table(name = "sec_bars")
public class SecBarEntity extends BaseBarEntity {

}
