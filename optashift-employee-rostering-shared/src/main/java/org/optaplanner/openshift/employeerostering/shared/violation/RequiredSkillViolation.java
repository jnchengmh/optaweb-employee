package org.optaplanner.openshift.employeerostering.shared.violation;

import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import org.optaplanner.openshift.employeerostering.shared.shift.Shift;

public class RequiredSkillViolation implements ConstraintMatchView {

    private Shift shift;
    private HardMediumSoftLongScore score;

    public RequiredSkillViolation() {

    }

    public RequiredSkillViolation(Shift shift, HardMediumSoftLongScore score) {
        this.shift = shift;
        this.score = score;
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public HardMediumSoftLongScore getScore() {
        return score;
    }

    public void setScore(HardMediumSoftLongScore score) {
        this.score = score;
    }
}
