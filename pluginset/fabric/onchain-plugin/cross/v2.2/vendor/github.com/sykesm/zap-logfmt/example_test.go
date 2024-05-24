package zaplogfmt_test

import (
	"os"

	zaplogfmt "github.com/sykesm/zap-logfmt"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
)

func Example_usage() {
	config := zap.NewProductionEncoderConfig()
	config.TimeKey = ""

	logger := zap.New(zapcore.NewCore(
		zaplogfmt.NewEncoder(config),
		os.Stdout,
		zapcore.DebugLevel,
	)).Named("main").With(zap.String("type", "greeting"))

	logger.Info("Hello World")

	// Output: level=info logger=main msg="Hello World" type=greeting
}

func Example_array() {
	config := zap.NewProductionEncoderConfig()
	config.TimeKey = ""

	logger := zap.New(zapcore.NewCore(
		zaplogfmt.NewEncoder(config),
		os.Stdout,
		zapcore.DebugLevel,
	))

	logger.Info("counting", zap.Ints("values", []int{0, 1, 2, 3}))

	// Output: level=info msg=counting values=[0,1,2,3]
}
