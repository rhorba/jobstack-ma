import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Company } from './company.model';
import { CheckoutSession } from './checkout.model';
import { JobPosting } from '../job/job-posting.model';

type ViewState = 'loading' | 'company-form' | 'job-form' | 'ready-to-pay' | 'checkout' | 'success' | 'failed';

@Component({
  selector: 'app-employer-home',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, RouterLink],
  templateUrl: './employer-home.component.html',
})
export class EmployerHomeComponent implements OnInit {
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);

  readonly email = signal('');
  readonly view = signal<ViewState>('loading');
  readonly company = signal<Company | null>(null);
  readonly draftJob = signal<JobPosting | null>(null);
  readonly checkoutSession = signal<CheckoutSession | null>(null);
  readonly error = signal<string | null>(null);
  readonly submitting = signal(false);

  companyForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    sector: ['', [Validators.required, Validators.maxLength(50)]],
    city: ['', [Validators.maxLength(100)]],
  });

  jobForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    sector: ['', [Validators.required, Validators.maxLength(50)]],
    city: ['', [Validators.required, Validators.maxLength(100)]],
    contractType: ['CDI', [Validators.required]],
    description: ['', [Validators.required]],
  });

  ngOnInit(): void {
    this.http.get<{ email: string }>('/api/v1/employers/me').subscribe((res) => this.email.set(res.email));

    this.http.get<Company>('/api/v1/employers/me/company').subscribe({
      next: (company) => {
        this.company.set(company);
        this.view.set('job-form');
      },
      error: () => this.view.set('company-form'),
    });
  }

  saveCompany(): void {
    if (this.companyForm.invalid) return;
    this.submitting.set(true);
    this.error.set(null);

    this.http.post<Company>('/api/v1/employers/me/company', this.companyForm.getRawValue()).subscribe({
      next: (company) => {
        this.company.set(company);
        this.submitting.set(false);
        this.view.set('job-form');
      },
      error: () => {
        this.submitting.set(false);
        this.error.set('Could not save your company. Please check the fields and try again.');
      },
    });
  }

  saveJob(): void {
    if (this.jobForm.invalid) return;
    this.submitting.set(true);
    this.error.set(null);

    this.http.post<JobPosting>('/api/v1/jobs', this.jobForm.getRawValue()).subscribe({
      next: (job) => {
        this.draftJob.set(job);
        this.submitting.set(false);
        this.view.set('ready-to-pay');
      },
      error: () => {
        this.submitting.set(false);
        this.error.set('Could not create the job posting. Please check the fields and try again.');
      },
    });
  }

  startCheckout(): void {
    const job = this.draftJob();
    if (!job) return;
    this.submitting.set(true);
    this.error.set(null);

    this.http.post<CheckoutSession>(`/api/v1/jobs/${job.id}/checkout`, {}).subscribe({
      next: (session) => {
        this.checkoutSession.set(session);
        this.submitting.set(false);
        this.view.set('checkout');
      },
      error: () => {
        this.submitting.set(false);
        this.error.set('Could not start checkout. Please try again.');
      },
    });
  }

  completeCheckout(outcome: 'SUCCESS' | 'FAILED'): void {
    const session = this.checkoutSession();
    if (!session) return;
    this.submitting.set(true);
    this.error.set(null);

    this.http.post<void>(`/api/v1/payments/${session.paymentId}/mock-outcome`, { outcome }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.view.set(outcome === 'SUCCESS' ? 'success' : 'failed');
      },
      error: () => {
        this.submitting.set(false);
        this.error.set('Could not confirm payment status. Please try again.');
      },
    });
  }

  retry(): void {
    this.view.set('checkout');
  }
}
