# training/train.py
import argparse
import json
import mlflow
import pandas as pd
from sklearn.linear_model import Ridge
from sklearn.model_selection import train_test_split

PHASES = ["early", "mid", "late"]

def train_phase(phase: str, data_dir: str, alpha: float):
    df = pd.read_csv(f"{data_dir}/samples_{phase}.csv")
    X = df.drop(columns=["outcome"])
    y = df["outcome"]

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    model = Ridge(alpha=alpha, fit_intercept=False)
    model.fit(X_train, y_train)

    train_r2 = model.score(X_train, y_train)
    test_r2 = model.score(X_test, y_test)

    mlflow.log_param(f"{phase}_alpha", alpha)
    mlflow.log_param(f"{phase}_n_samples", len(df))
    mlflow.log_metric(f"{phase}_train_r2", train_r2)
    mlflow.log_metric(f"{phase}_test_r2", test_r2)

    return model.coef_.tolist()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-dir", default="../data/v3_evaluator")
    parser.add_argument("--out", default="../weights/v3_evaluator/weights_v3_candidate.json")
    parser.add_argument("--alpha", type=float, default=1.0)
    parser.add_argument("--run-name", default="training-run-v3-eval-alpha-1-weights-v3-candidate")
    args = parser.parse_args()

    mlflow.set_experiment("othello-eval-weights")
    with mlflow.start_run(run_name=args.run_name):
        weights = {phase: train_phase(phase, args.data_dir, args.alpha) for phase in PHASES}

        with open(args.out, "w") as f:
            json.dump(weights, f, indent=2)

        mlflow.log_artifact(args.out)
        print(f"Wrote weights to {args.out}")

if __name__ == "__main__":
    main()