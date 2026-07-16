import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { EmployerHomeComponent } from './employer-home.component';
import { Company } from './company.model';
import { JobPosting } from '../job/job-posting.model';

describe('EmployerHomeComponent', () => {
  let fixture: ComponentFixture<EmployerHomeComponent>;
  let component: EmployerHomeComponent;
  let httpMock: HttpTestingController;

  const company: Company = { id: 'c1', name: 'Atlas Automotive SARL', sector: 'automotive', city: 'Tangier', verified: false };
  const draftJob: JobPosting = {
    id: 'j1',
    title: 'Automotive QA Engineer',
    description: 'desc',
    sector: 'automotive',
    city: 'Tangier',
    contractType: 'CDI',
    status: 'DRAFT',
    companyName: 'Atlas Automotive SARL',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [EmployerHomeComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(EmployerHomeComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function loadWithCompany() {
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/employers/me').flush({ email: 'employer@test.ma' });
    httpMock.expectOne('/api/v1/employers/me/company').flush(company);
  }

  function loadWithoutCompany() {
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/employers/me').flush({ email: 'employer@test.ma' });
    httpMock.expectOne('/api/v1/employers/me/company').flush('not found', { status: 404, statusText: 'Not Found' });
  }

  it('shows the company form when the employer has no company yet', () => {
    loadWithoutCompany();
    expect(component.view()).toBe('company-form');
  });

  it('shows the job form directly when a company already exists', () => {
    loadWithCompany();
    expect(component.view()).toBe('job-form');
    expect(component.company()).toEqual(company);
  });

  it('creates the company then moves to the job form', () => {
    loadWithoutCompany();

    component.companyForm.setValue({ name: 'Atlas Automotive SARL', sector: 'automotive', city: 'Tangier' });
    component.saveCompany();

    const req = httpMock.expectOne('/api/v1/employers/me/company');
    expect(req.request.method).toBe('POST');
    req.flush(company);

    expect(component.view()).toBe('job-form');
  });

  it('creates the draft job then moves to ready-to-pay', () => {
    loadWithCompany();

    component.jobForm.setValue({
      title: 'Automotive QA Engineer',
      sector: 'automotive',
      city: 'Tangier',
      contractType: 'CDI',
      description: 'Inspect assembly line output.',
    });
    component.saveJob();

    const req = httpMock.expectOne('/api/v1/jobs');
    expect(req.request.method).toBe('POST');
    req.flush(draftJob);

    expect(component.view()).toBe('ready-to-pay');
    expect(component.draftJob()).toEqual(draftJob);
  });

  it('starts checkout and shows the mock payment screen', () => {
    loadWithCompany();
    component.draftJob.set(draftJob);
    component.view.set('ready-to-pay');

    component.startCheckout();

    const req = httpMock.expectOne('/api/v1/jobs/j1/checkout');
    expect(req.request.method).toBe('POST');
    req.flush({ paymentId: 'p1', transactionId: 'MOCK-1', redirectUrl: '/checkout/p1', amount: 490 });

    expect(component.view()).toBe('checkout');
    expect(component.checkoutSession()?.paymentId).toBe('p1');
  });

  it('completing checkout with SUCCESS shows the success screen', () => {
    loadWithCompany();
    component.checkoutSession.set({ paymentId: 'p1', transactionId: 'MOCK-1', redirectUrl: '/checkout/p1', amount: 490 });
    component.view.set('checkout');

    component.completeCheckout('SUCCESS');

    const req = httpMock.expectOne('/api/v1/payments/p1/mock-outcome');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ outcome: 'SUCCESS' });
    req.flush(null);

    expect(component.view()).toBe('success');
  });

  it('completing checkout with FAILED shows the failed screen with a retry option', () => {
    loadWithCompany();
    component.checkoutSession.set({ paymentId: 'p1', transactionId: 'MOCK-1', redirectUrl: '/checkout/p1', amount: 490 });
    component.view.set('checkout');

    component.completeCheckout('FAILED');
    httpMock.expectOne('/api/v1/payments/p1/mock-outcome').flush(null);

    expect(component.view()).toBe('failed');

    component.retry();
    expect(component.view()).toBe('checkout');
  });
});
