package ftc.evlib.hardware.motors;

import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.Collection;

import ftc.evlib.util.StepTimer;
import ftc.electronvolts.util.Function;
import ftc.electronvolts.util.InputExtractor;
import ftc.electronvolts.util.Utility;

/**
 * This file was made by the electronVolts, FTC team 7393
 * Date Created: 9/11/16
 *
 * Factory class for creating Motor wrapper classes.
 * Has methods for all the combinations of with/without encoders and forward/reversed.
 *
 * @see ftc.evlib.hardware.motors.Motor
 * @see MotorEnc
 */
public class Motors {
    /**
     * Get an InputExtractor for the power of a motor
     *
     * @param motor the motor
     * @return the created InputExtractor
     */
    public static InputExtractor<Double> powerIE(final ftc.evlib.hardware.motors.Motor motor) {
        return new InputExtractor<Double>() {
            @Override
            public Double getValue() {
                return motor.getPower();
            }
        };
    }

    /**
     * Get an InputExtractor for the mode of a motor
     *
     * @param motor the motor
     * @return the created InputExtractor
     */
    public static InputExtractor<ftc.evlib.hardware.motors.Motor.Mode> modeIE(final ftc.evlib.hardware.motors.Motor motor) {
        return new InputExtractor<ftc.evlib.hardware.motors.Motor.Mode>() {
            @Override
            public ftc.evlib.hardware.motors.Motor.Mode getValue() {
                return motor.getMode();
            }
        };
    }

    /**
     * Get an InputExtractor for the encoder position of a motor with an encoder
     *
     * @param motorEnc the motor
     * @return the created InputExtractor
     */
    public static InputExtractor<Integer> encoderIE(final MotorEnc motorEnc) {
        return new InputExtractor<Integer>() {
            @Override
            public Integer getValue() {
                return motorEnc.getEncoderPosition();
            }
        };
    }

    /**
     * combine two motors with encoders into one motor
     *
     * @param motorEnc1 the first motor (with encoder)
     * @param motorEnc2 the second motor (with encoder)
     * @return the motor that controls both (with encoder support)
     */
    public static MotorEnc combinedWithEncoder(MotorEnc motorEnc1, MotorEnc motorEnc2) {
        return combinedWithEncoder(ImmutableList.of(motorEnc1, motorEnc2));
    }

    /**
     * combines any number of motors with encoders into one
     *
     * @param motorEncs the list of motors to combine (all must have encoders)
     * @return the motor that controls all of them (with encoder support)
     */
    public static MotorEnc combinedWithEncoder(final Collection<MotorEnc> motorEncs) {
        return new MotorEnc() {
            @Override
            public void setSpeed(double speed) {
                for (MotorEnc motorEnc : motorEncs)
                    motorEnc.setSpeed(speed);
            }

            @Override
            public void setPosition(int encoderTarget, double maxCorrectionPower) {
                for (MotorEnc motorEnc : motorEncs)
                    motorEnc.setPosition(encoderTarget, maxCorrectionPower);
            }

            @Override
            public void resetEncoder() {
                for (MotorEnc motorEnc : motorEncs) motorEnc.resetEncoder();
            }

            @Override
            public int getEncoderPosition() {
                if (motorEncs.isEmpty()) {
                    return 0;
                } else {
                    int total = 0;
                    for (MotorEnc motorEnc : motorEncs) total += motorEnc.getEncoderPosition();
                    return total / motorEncs.size();
                }
            }

            @Override
            public void setPower(double power) {
                for (MotorEnc motorEnc : motorEncs) motorEnc.setPower(power);
            }

            @Override
            public double getPower() {
                if (motorEncs.isEmpty()) {
                    return 0;
                } else {
                    return motorEncs.iterator().next().getPower();
                }
            }

            @Override
            public Mode getMode() {
                if (motorEncs.isEmpty()) {
                    return Mode.POWER;
                } else {
                    return motorEncs.iterator().next().getMode();
                }
            }

            @Override
            public void update() {
                for (MotorEnc motorEnc : motorEncs) motorEnc.update();
            }
        };
    }

    /**
     * combine two motors with or without encoders into one motor
     *
     * @param motor1 the first motor
     * @param motor2 the second motor
     * @return the motor that controls both (without encoder support)
     */
    public static ftc.evlib.hardware.motors.Motor combinedWithoutEncoder(ftc.evlib.hardware.motors.Motor motor1, ftc.evlib.hardware.motors.Motor motor2) {
        return combinedWithoutEncoder(ImmutableList.of(motor1, motor2));
    }

    /**
     * combines any number of motors with or without encoders into one
     *
     * @param motors the list of motors to combine
     * @return the motor that controls all of them (without encoder support)
     */
    public static ftc.evlib.hardware.motors.Motor combinedWithoutEncoder(final Collection<ftc.evlib.hardware.motors.Motor> motors) {
        return new ftc.evlib.hardware.motors.Motor() {
            @Override
            public void setPower(double power) {
                for (ftc.evlib.hardware.motors.Motor motor : motors) motor.setPower(power);
            }

            @Override
            public double getPower() {
                if (motors.isEmpty()) {
                    return 0;
                } else {
                    return motors.iterator().next().getPower();
                }
            }

            @Override
            public Mode getMode() {
                return Mode.POWER;
            }

            @Override
            public void update() {
                for (ftc.evlib.hardware.motors.Motor motor : motors) motor.update();
            }
        };
    }

    /**
     * Initialize a dcMotor by setting parameters and checking that they were set properly
     *
     * @param dcMotor  the motor to initialize
     * @param reversed whether or not the motor direction should be reversed
     * @param brake    whether to brake or float when stopping
     * @param runMode  what mode to start the motor in
     */
    private static void dcMotorInit(DcMotor dcMotor, boolean reversed, boolean brake, DcMotor.RunMode runMode) {
        //reset the encoder position to zero
        do {
            dcMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        } while (dcMotor.getMode() != DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        //determine the motor's direction as a Direction object
        DcMotor.Direction direction;
        if (reversed) {
            direction = DcMotorSimple.Direction.REVERSE;
        } else {
            direction = DcMotorSimple.Direction.FORWARD;
        }

        //set the motor's direction
        do {
            dcMotor.setDirection(direction);
        } while (dcMotor.getDirection() != direction);

        //set the motor's mode
        do {
            dcMotor.setMode(runMode);
        } while (dcMotor.getMode() != runMode);

        //determine the ZeroPowerBehavior
        DcMotor.ZeroPowerBehavior zeroPowerBehavior;
        if (brake) {
            zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE;
        } else {
            zeroPowerBehavior = DcMotor.ZeroPowerBehavior.FLOAT;
        }

        //set the motor's ZeroPowerBehavior
        do {
            dcMotor.setZeroPowerBehavior(zeroPowerBehavior);
        } while (dcMotor.getZeroPowerBehavior() != zeroPowerBehavior);
    }

    /**
     * Create a Motor from the hardware map
     *
     * @param hardwareMap the hardwareMap from the opmode
     * @param dcMotorName the name of the DcMotor in the hardwareMap
     * @param reversed    true if the motor's direction should be reversed
     * @param brake       true if the motor should brake when stopped
     * @param stoppers    the Stoppers object to add the motor to
     * @return the created MotorEnc
     */
    public static ftc.evlib.hardware.motors.Motor withoutEncoder(HardwareMap hardwareMap, String dcMotorName, boolean reversed, boolean brake, Stoppers stoppers) {
        return withoutEncoder(hardwareMap.dcMotor.get(dcMotorName), reversed, brake, stoppers);
    }

    /**
     * Create a Motor from a DcMotor
     *
     * @param dcMotor  the DcMotor to be wrapped
     * @param reversed true if the motor's direction should be reversed
     * @param brake    true if the motor should brake when stopped
     * @param stoppers the Stoppers object to add the motor to
     * @return the created MotorEnc
     */
    public static ftc.evlib.hardware.motors.Motor withoutEncoder(final DcMotor dcMotor, boolean reversed, boolean brake, Stoppers stoppers) {
        //initialize the motor with no encoder
        dcMotorInit(dcMotor, reversed, brake, DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        stoppers.add(new Stopper() {
            @Override
            public void stop() {
                do {
                    dcMotor.setPower(0);
                } while (dcMotor.getPower() != 0);
            }
        });

        return new ftc.evlib.hardware.motors.Motor() {
            private double power = 0,lastPower=0;

            @Override
            public void setPower(double power) {
                this.power = power;
            }

            @Override
            public double getPower() {
                return power;
            }

            @Override
            public Mode getMode() {
                return Mode.POWER;
            }

            @Override
            public void update() {
                if(power!=lastPower) {
                    lastPower=power;
                    dcMotor.setPower(Utility.motorLimit(power));
                }
            }
        };
    }

    /**
     * Convert a Motor.MotorMode to a DcMotor.RunMode
     *
     * @param mode the mode to convert
     * @return the corresponding DcMotor.RunMode
     */
    public static DcMotor.RunMode motorModeToDcMotorRunMode(ftc.evlib.hardware.motors.Motor.Mode mode) {
        switch (mode) {
            case POWER:
                return DcMotor.RunMode.RUN_WITHOUT_ENCODER;
            case SPEED:
                return DcMotor.RunMode.RUN_USING_ENCODER;
            case POSITION:
                return DcMotor.RunMode.RUN_TO_POSITION;
            default:
                return null;
        }
    }

    /**
     * Convert a DcMotor.RunMode to a Motor.Mode
     *
     * @param runMode the mode to convert
     * @return the corresponding Motor.Mode
     */
    public static ftc.evlib.hardware.motors.Motor.Mode dcMotorRunModeToMotorMode(DcMotor.RunMode runMode) {
        switch (runMode) {
            case RUN_WITHOUT_ENCODER:
                return ftc.evlib.hardware.motors.Motor.Mode.POWER;
            case RUN_USING_ENCODER:
                return ftc.evlib.hardware.motors.Motor.Mode.SPEED;
            case RUN_TO_POSITION:
                return ftc.evlib.hardware.motors.Motor.Mode.POSITION;
            default:
                return null;
        }
    }

    /**
     * Create a MotorEnc from the hardware map
     *
     * @param hardwareMap              the hardwareMap from the opmode
     * @param dcMotorName              the name of the DcMotor in the hardwareMap
     * @param reversed                 true if the motor's direction should be reversed
     * @param brake                    true if the motor should brake when stopped
     * @param stoppers                 the Stoppers object to add the motor to
     * @return the created MotorEnc
     */
    public static MotorEnc withEncoder(HardwareMap hardwareMap, String dcMotorName, boolean reversed, boolean brake, Stoppers stoppers) {
        return withEncoder(hardwareMap.dcMotor.get(dcMotorName), reversed, brake, stoppers);
    }

    /**
     * Create a MotorEnc from a DcMotor
     *
     * @param dcMotor                  the DcMotor to be wrapped
     * @param reversed                 true if the motor's direction should be reversed
     * @param brake                    true if the motor should brake when stopped
     * @param stoppers                 the Stoppers object to add the motor to
     * @return the created MotorEnc
     */
    public static MotorEnc withEncoder(final DcMotor dcMotor, boolean reversed, boolean brake, Stoppers stoppers) {
        final ftc.evlib.hardware.motors.Motor.Mode initMode = ftc.evlib.hardware.motors.Motor.Mode.SPEED;
        dcMotorInit(dcMotor, reversed, brake, motorModeToDcMotorRunMode(initMode)); //start with speed mode

//        do {
//            dcMotor.setMaxSpeed(maxEncoderTicksPerSecond);
//        } while (dcMotor.getMaxSpeed() != maxEncoderTicksPerSecond);

        stoppers.add(new Stopper() {
            @Override
            public void stop() {
                try {
                    do {
                        dcMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                    } while (dcMotor.getMode() != DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                    do {
                        dcMotor.setPower(0);
                    } while (dcMotor.getPower() != 0);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });

        return new MotorEnc() {
            private int encoderZero = 0, encoderPosition = 0;
            private Mode mode = initMode, lastMode = initMode;
            private double power = 0,lastPower=0;
            private int encoderTarget = 0;

            @Override
            public void setPower(double power) {
                mode = Mode.POWER;
                this.power = power;
            }

            @Override
            public double getPower() {
                return power;
            }

            @Override
            public void setSpeed(double speed) {
                mode = Mode.SPEED;
                power = speed;
            }

            @Override
            public void setPosition(int encoderTarget, double maxCorrectionPower) {
                mode = Mode.POSITION;
                this.encoderTarget = encoderTarget;
                power = maxCorrectionPower;
            }

            @Override
            public void resetEncoder() {
                encoderZero = encoderPosition;
            }

            @Override
            public int getEncoderPosition() {
                return encoderPosition - encoderZero;
            }

            @Override
            public Mode getMode() {
                return mode;
            }

            StepTimer t = new StepTimer("MotorRoot", Log.VERBOSE);

            @Override
            public void update() {
                t.start();
                t.step("motor_encoder_position");
                encoderPosition = dcMotor.getCurrentPosition();

                t.step("motor_setMode");

                if (mode != lastMode) {

                    dcMotor.setMode(motorModeToDcMotorRunMode(mode));
                    lastMode = dcMotorRunModeToMotorMode(dcMotor.getMode());
                }

                t.step("motor_setTargetPosition");

                if (mode == Mode.POSITION) {
                    dcMotor.setTargetPosition(encoderTarget);
                }

                t.step("motor_setPower");
                if(power!=lastPower) {
                    lastPower=power;
                    dcMotor.setPower(Utility.motorLimit(power));
                }
                t.stop();
            }
        };
    }

    /**
     * Wraps a continuous rotation servo as a normal motor
     *
     * @param crServo  the servo to be wrapped as a motor
     * @param reversed true if the servo should be reversed
     * @return the Motor wrapper class
     */
    public static ftc.evlib.hardware.motors.Motor continuousServo(final CRServo crServo, boolean reversed) {
        DcMotorSimple.Direction direction;
        if (reversed) {
            direction = DcMotorSimple.Direction.REVERSE;
        } else {
            direction = DcMotorSimple.Direction.FORWARD;
        }

        do {
            crServo.setDirection(direction);
        } while (crServo.getDirection() != direction);

        return new ftc.evlib.hardware.motors.Motor() {
            private double power = 0;

            @Override
            public void setPower(double power) {
                this.power = power;
            }

            @Override
            public double getPower() {
                return power;
            }

            @Override
            public Mode getMode() {
                return Mode.POWER;
            }

            @Override
            public void update() {
                crServo.setPower(power);
            }
        };
    }

    /**
     * Scale a motor's power by a constant
     *
     * @param motor the motor to scale
     * @param scale the scaling factor
     * @return the created MotorEnc
     */
    public static ftc.evlib.hardware.motors.Motor scale(final ftc.evlib.hardware.motors.Motor motor, final double scale) {
        return new ftc.evlib.hardware.motors.Motor() {
            @Override
            public void setPower(double power) {
                motor.setPower(power * scale);
            }

            @Override
            public double getPower() {
                return motor.getPower();
            }

            @Override
            public Mode getMode() {
                return motor.getMode();
            }

            @Override
            public void update() {
                motor.update();
            }

        };
    }

    /**
     * Scale a motor's power and speed by a constant
     *
     * @param motorEnc the motor to scale
     * @param scale    the scaling factor
     * @return the created MotorEnc
     */
    public static ftc.evlib.hardware.motors.MotorEnc scale(final MotorEnc motorEnc, final double scale) {
        return new MotorEnc() {
            @Override
            public void setSpeed(double speed) {
                motorEnc.setSpeed(speed * scale);
            }

            @Override
            public void setPosition(int encoderTarget, double maxCorrectionPower) {
                motorEnc.setPosition(encoderTarget, maxCorrectionPower);
            }

            @Override
            public void resetEncoder() {
                motorEnc.resetEncoder();
            }

            @Override
            public int getEncoderPosition() {
                return motorEnc.getEncoderPosition();
            }

            @Override
            public void setPower(double power) {
                motorEnc.setPower(power * scale);
            }

            @Override
            public double getPower() {
                return motorEnc.getPower() / scale;
            }

            @Override
            public Mode getMode() {
                return motorEnc.getMode();
            }

            @Override
            public void update() {
                motorEnc.update();
            }

        };
    }


    /**
     * Scale a motor's power by a function
     *
     * @param motor    the motor to scale
     * @param function the function to scale by
     * @return the created MotorEnc
     */
    public static ftc.evlib.hardware.motors.Motor scale(final ftc.evlib.hardware.motors.Motor motor, final Function function) {
        return new ftc.evlib.hardware.motors.Motor() {
            private double power = 0;

            @Override
            public void setPower(double power) {
                this.power = power;
                motor.setPower(function.f(power));
            }

            @Override
            public double getPower() {
                return power;
            }

            @Override
            public Mode getMode() {
                return motor.getMode();
            }

            @Override
            public void update() {
                motor.update();
            }

        };
    }

    /**
     * Scale a motor's power and speed by a function
     *
     * @param motorEnc the motor to scale
     * @param function the function to scale by
     * @return the created MotorEnc
     */
    public static ftc.evlib.hardware.motors.Motor scale(final MotorEnc motorEnc, final Function function) {
        return new MotorEnc() {
            private double power = 0;

            @Override
            public void setSpeed(double speed) {
                motorEnc.setSpeed(function.f(speed));
            }

            @Override
            public void setPosition(int encoderTarget, double maxCorrectionPower) {
                motorEnc.setPosition(encoderTarget, maxCorrectionPower);
            }

            @Override
            public void resetEncoder() {
                motorEnc.resetEncoder();
            }

            @Override
            public int getEncoderPosition() {
                return motorEnc.getEncoderPosition();
            }


            @Override
            public void setPower(double power) {
                this.power = power;
                motorEnc.setPower(function.f(power));
            }

            @Override
            public double getPower() {
                return power;
            }

            @Override
            public Mode getMode() {
                return motorEnc.getMode();
            }

            @Override
            public void update() {
                motorEnc.update();
            }

        };
    }

    /**
     * @return a motor with an encoder that does nothing
     */
    public static MotorEnc dummyWithEncoder() {
        return new MotorEnc() {
            private Mode mode = Mode.POWER;

            @Override
            public void setSpeed(double speed) {
                mode = Mode.SPEED;
            }

            @Override
            public void setPosition(int encoderTarget, double maxCorrectionPower) {
                mode = Mode.POSITION;
            }

            @Override
            public void resetEncoder() {

            }

            @Override
            public int getEncoderPosition() {
                return 0;
            }

            @Override
            public void setPower(double power) {
                mode = Mode.POWER;
            }

            @Override
            public double getPower() {
                return 0;
            }

            @Override
            public Mode getMode() {
                return mode;
            }

            @Override
            public void update() {

            }
        };
    }

    /**
     * @return a motor that does nothing
     */
    public static ftc.evlib.hardware.motors.Motor dummyWithoutEncoder() {
        return new ftc.evlib.hardware.motors.Motor() {
            @Override
            public void setPower(double power) {

            }

            @Override
            public double getPower() {
                return 0;
            }

            @Override
            public Mode getMode() {
                return Mode.POWER;
            }

            @Override
            public void update() {

            }
        };
    }

//    /**
//     * Simulate a NeveRest20 with an encoder
//     *
//     * @return the simulated motor
//     */
//    public static MotorEnc simulatedNeveRest20() {
//        return simulatedNeveRest(20);
//    }
//
//    /**
//     * Simulate a NeveRest40 with an encoder
//     *
//     * @return the simulated motor
//     */
//    public static MotorEnc simulatedNeveRest40() {
//        return simulatedNeveRest(40);
//    }
//
//    /**
//     * Simulate a NeveRest60 with an encoder
//     *
//     * @return the simulated motor
//     */
//    public static MotorEnc simulatedNeveRest60() {
//        return simulatedNeveRest(60);
//    }
//
//    /**
//     * Simulate a NeveRest motor of any gear ratio with an encoder
//     *
//     * @param gearRatio the gear ratio of the motor
//     * @return the simulated motor
//     */
//    public static MotorEnc simulatedNeveRest(double gearRatio) {
//        return simulatedMotor(6400 / gearRatio, 7 * gearRatio);
//    }

//    /**
//     * Simulate a motor with an encoder by calculating the encoder position as the integral of the motor speed
//     *
//     * @param revolutionsPerMinute      motor speed in rpm
//     * @param encoderTicksPerRevolution encoder ticks per turn
//     * @return the simulated motor
//     */
//    public static MotorEnc simulatedMotor(final double revolutionsPerMinute, final double encoderTicksPerRevolution) {
//        return new MotorEnc() {
//            private int encoderTicks = 0, targetEncoderPosition = 0;
//            private double power = 0, targetPower = 0, powerError = 0;
//            private MotorMode mode = MotorMode.POWER;
//            private StopBehavior stopBehavior = StopBehavior.BRAKE;
//            private long lastTime = System.currentTimeMillis();
//
//            private void setPower1(double power) {
//                targetPower = Utility.motorLimit(power);
//                powerError = targetPower - this.power;
//            }
//
//            @Override
//            public void setSpeed(double speed) {
//                update();
//                mode = MotorMode.SPEED;
//                setPower1(speed);
//            }
//
//            @Override
//            public void setPosition(int encoderPosition) {
//                update();
//                mode = MotorMode.POSITION;
//                targetEncoderPosition = encoderPosition;
//                setPower1(Math.signum(targetEncoderPosition - encoderTicks));
//            }
//
//            @Override
//            public void resetEncoder() {
//                update();
//                encoderTicks = 0;
//            }
//
//            @Override
//            public int getEncoderPosition() {
//                update();
//                return encoderTicks;
//            }
//
//            @Override
//            public void setPower(double power) {
//                update();
//                mode = MotorMode.POWER;
//                setPower1(power);
//            }
//
//            @Override
//            public MotorMode getMode() {
//                update();
//                return mode;
//            }
//
//            @Override
//            public void setStopBehavior(StopBehavior stopBehavior) {
//                update();
//                this.stopBehavior = stopBehavior;
//            }
//
//            private static final double MILLISECOND_PER_MIN = 60 * 1000;
//            private static final double K = -0.01; //related to the moment of inertia of the motor
//
//            private void update() {
////                long now = System.currentTimeMillis();
////                long deltaTime = lastTime - now;
////
////                double maxTicks = deltaTime / MILLISECOND_PER_MIN * revolutionsPerMinute * encoderTicksPerRevolution;
////
////                if (mode == MotorMode.POSITION) {
////                    int encoderError = targetEncoderPosition - encoderTicks;
////                    if (maxTicks >= Math.abs(encoderError)) {
////                        if (maxTicks == 0) {
////                            targetPower = 0;
////                        } else {
////                            targetPower = encoderError / maxTicks;
////                        }
////                    } else {
////                        targetPower = Math.signum(encoderError);
////                    }
////                }
////                power = targetPower;
////                encoderTicks += power * maxTicks;
////
////                lastTime = now;
//
//                long now = System.currentTimeMillis();
//                double deltaTimeMin = (lastTime - now) / MILLISECOND_PER_MIN;
//
//                powerError *= Math.exp(deltaTimeMin * K);
//                power = targetPower - powerError;
//
//                double maxTicks = (deltaTimeMin * targetPower - powerError) * revolutionsPerMinute * encoderTicksPerRevolution;
//
//                if (mode == MotorMode.POSITION) {
//                    int encoderError = targetEncoderPosition - encoderTicks;
//                    if (maxTicks >= encoderError) {
//                        power = 0;
//                        encoderTicks += encoderError;
//                    } else {
//                        encoderTicks += maxTicks;
//                    }
//                } else {
//                    encoderTicks += maxTicks;
//                }
//
//                lastTime = now;
//            }
//        };
//    }
}
