package com.mvpiq.service.ia;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
class Candidate {
    double cx, cy;
    double probability;
    double bw, bh;
    double score;
    double distance;
}
