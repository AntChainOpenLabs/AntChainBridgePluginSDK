package zaplogfmt_test

import (
	"os"

	zaplogfmt "github.com/sykesm/zap-logfmt"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
)

type Person struct {
	First string
	Last  string
	Age   int
}

func (p Person) MarshalLogObject(enc zapcore.ObjectEncoder) error {
	enc.AddString("first", p.First)
	enc.AddString("last", p.Last)
	enc.AddInt("age", p.Age)
	return nil
}

func Example_object() {
	config := zap.NewProductionEncoderConfig()
	config.TimeKey = ""

	logger := zap.New(zapcore.NewCore(
		zaplogfmt.NewEncoder(config),
		os.Stdout,
		zapcore.DebugLevel,
	))

	person := Person{First: "Arthur", Last: "Dent", Age: 42}
	logger.Warn("hitchhiker discovered", zap.Object("identity", person))

	// Output: level=warn msg="hitchhiker discovered" identity="first=Arthur last=Dent age=42"
}
